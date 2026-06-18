package com.jobmatch.service;

import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.enums.JobStatus;
import com.jobmatch.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final WeeklyStatsRepository weeklyStatsRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM: expire jobs
    @Transactional
    public void expireJobs() {
        log.info("Running scheduled job: Expire old jobs");
        int expiryDays = getSettingAsInt("job_expiry_days", 30);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(expiryDays);

        List<Job> expiredJobs = jobRepository.findByStatusAndDeletedFalse(JobStatus.ACTIVE, PageRequest.of(0, 1000))
                .stream()
                .filter(job -> job.getCreatedAt() != null && job.getCreatedAt().isBefore(cutoff))
                .collect(Collectors.toList());

        for (Job job : expiredJobs) {
            job.setStatus(JobStatus.EXPIRED);
        }
        jobRepository.saveAll(expiredJobs);
        log.info("Expired {} jobs", expiredJobs.size());
    }

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM: delete expired tokens
    @Transactional
    public void deleteExpiredVerificationTokens() {
        log.info("Running scheduled job: Delete expired verification tokens");
        int cleanupDays = getSettingAsInt("token_cleanup_days", 7);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupDays);

        List<com.jobmatch.domain.entity.User> users = userRepository.findByVerificationTokenExpiryBefore(cutoff);
        for (com.jobmatch.domain.entity.User user : users) {
            user.setVerificationToken(null);
            user.setVerificationTokenExpiry(null);
        }
        userRepository.saveAll(users);
        log.info("Cleaned expired tokens for {} users", users.size());
    }

    @Scheduled(cron = "0 30 3 * * ?") // Daily at 3:30 AM: delete expired password reset tokens
    @Transactional
    public void deleteExpiredPasswordResetTokens() {
        log.info("Running scheduled job: Delete expired password reset tokens");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        List<com.jobmatch.domain.entity.User> users = userRepository.findByPasswordResetTokenExpiryBefore(cutoff);
        for (com.jobmatch.domain.entity.User user : users) {
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
        }
        userRepository.saveAll(users);
        log.info("Cleaned expired password reset tokens for {} users", users.size());
    }

    @Scheduled(cron = "0 0 1 * * MON") // Every Monday at 1 AM: weekly stats
    @Transactional
    public void generateWeeklyStatistics() {
        log.info("Running scheduled job: Generate weekly statistics");
        if (!isFeatureEnabled("weekly_stats_enabled")) {
            log.info("Weekly stats generation is disabled");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = weekEnd.plusDays(1).atStartOfDay();

        long newJobs = jobRepository.countByStatusAndDeletedFalse(JobStatus.ACTIVE);
        long newApplications = applicationRepository.countByCreatedAtBetween(weekStartDateTime, weekEndDateTime);
        long newUsers = userRepository.countByCreatedAtBetween(weekStartDateTime, weekEndDateTime);
        long newRecruiters = userRepository.countByRoleAndCreatedAtBetween("ROLE_RECRUITER", weekStartDateTime, weekEndDateTime);

        com.jobmatch.domain.entity.WeeklyStats stats = weeklyStatsRepository.findByWeekStart(weekStart)
                .orElse(new com.jobmatch.domain.entity.WeeklyStats());

        stats.setWeekStart(weekStart);
        stats.setWeekEnd(weekEnd);
        stats.setNewJobs((int) newJobs);
        stats.setNewApplications((int) newApplications);
        stats.setNewUsers((int) newUsers);
        stats.setNewRecruiters((int) newRecruiters);
        stats.setActiveJobs((int) jobRepository.countByStatusAndDeletedFalse(JobStatus.ACTIVE));
        stats.setTotalApplications((int) applicationRepository.count());
        stats.setHiredCount((int) applicationRepository.countByStatus("HIRED"));

        weeklyStatsRepository.save(stats);
        log.info("Weekly statistics generated for week: {}", weekStart);
    }

    @Scheduled(cron = "0 0 0 1 * ?") // First day of every month at midnight: monthly reports
    @Transactional
    public void generateMonthlyReport() {
        log.info("Running scheduled job: Generate monthly report");
        if (!isFeatureEnabled("monthly_reports_enabled")) {
            log.info("Monthly report generation is disabled");
            return;
        }

        LocalDate now = LocalDate.now();
        String monthYear = String.format("%d-%02d", now.getYear(), now.getMonthValue());

        LocalDateTime monthStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = now.withDayOfMonth(now.lengthOfMonth()).plusDays(1).atStartOfDay();

        long totalJobs = jobRepository.countByStatusAndDeletedFalse(JobStatus.ACTIVE);
        long totalApplications = applicationRepository.count();
        long totalUsers = userRepository.countByDeletedFalse();
        long totalRecruiters = userRepository.countByRoleAndDeletedFalse("ROLE_RECRUITER");

        BigDecimal appsPerJob = totalJobs > 0
                ? BigDecimal.valueOf(totalApplications).divide(BigDecimal.valueOf(totalJobs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long hiredCount = applicationRepository.countByStatusAndCreatedAtBetween("HIRED", monthStart, monthEnd);
        long totalProcessed = applicationRepository.countByCreatedAtBetween(monthStart, monthEnd);
        BigDecimal conversionRate = totalProcessed > 0
                ? BigDecimal.valueOf(hiredCount).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalProcessed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get top skills (from required_skills JSON in jobs)
        List<String> allSkills = new ArrayList<>();
        jobRepository.findByStatusAndDeletedFalse(JobStatus.ACTIVE, PageRequest.of(0, 1000))
                .forEach(job -> {
                    if (job.getRequiredSkills() != null) {
                        allSkills.addAll(job.getRequiredSkills());
                    }
                });
        Map<String, Long> skillCounts = allSkills.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        String topSkills = skillCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));

        // Recruiter performance
        String recruiterPerf = "Monthly report for " + monthYear + " - Total jobs: " + totalJobs;

        com.jobmatch.domain.entity.MonthlyReport report = monthlyReportRepository.findByMonthYear(monthYear)
                .orElse(new com.jobmatch.domain.entity.MonthlyReport());

        report.setMonthYear(monthYear);
        report.setTotalJobs((int) totalJobs);
        report.setTotalApplications((int) totalApplications);
        report.setTotalUsers((int) totalUsers);
        report.setTotalRecruiters((int) totalRecruiters);
        report.setApplicationsPerJob(appsPerJob);
        report.setHiringConversionRate(conversionRate);
        report.setTopSkills(topSkills);
        report.setRecruiterPerformance(recruiterPerf);

        monthlyReportRepository.save(report);
        log.info("Monthly report generated for: {}", monthYear);
    }

    private int getSettingAsInt(String key, int defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> {
                    try {
                        return Integer.parseInt(s.getSettingValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private boolean isFeatureEnabled(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                .orElse(true);
    }
}