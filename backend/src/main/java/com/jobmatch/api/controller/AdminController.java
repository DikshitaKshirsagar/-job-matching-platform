package com.jobmatch.api.controller;

import com.jobmatch.domain.entity.*;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.*;
import com.jobmatch.service.ReportingService;
import com.jobmatch.util.UserIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin", description = "Admin console management endpoints")
public class AdminController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final AuditLogRepository auditLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ReportingService reportingService;
    private final UserIdResolver userIdResolver;

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/disable")
    @Operation(summary = "Disable a user")
    @Transactional
    public ResponseEntity<Map<String, Object>> disableUser(@PathVariable Long id) {
        if (userRepository.existsByIdAndDeletedFalse(id)) {
            userRepository.findById(id).ifPresent(user -> {
                user.setDeleted(true);
                userRepository.save(user);
                log.info("Admin disabled user id: {}", id);
            });
            return ResponseEntity.ok(okResponse("User disabled successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/users/{id}/enable")
    @Operation(summary = "Enable a user")
    @Transactional
    public ResponseEntity<Map<String, Object>> enableUser(@PathVariable Long id) {
        if (userRepository.findById(id).isPresent()) {
            userRepository.findById(id).ifPresent(user -> {
                user.setDeleted(false);
                userRepository.save(user);
                log.info("Admin enabled user id: {}", id);
            });
            return ResponseEntity.ok(okResponse("User enabled successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/recruiters")
    @Operation(summary = "Get all recruiters")
    public ResponseEntity<List<User>> getRecruiters() {
        List<User> recruiters = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ROLE_RECRUITER)
                .toList();
        return ResponseEntity.ok(recruiters);
    }

    @GetMapping("/jobs")
    @Operation(summary = "Get all jobs")
    public ResponseEntity<Page<Job>> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(jobRepository.findAll(pageable));
    }

    @PutMapping("/jobs/{id}/disable")
    @Operation(summary = "Disable a job posting")
    @Transactional
    public ResponseEntity<Map<String, Object>> disableJob(@PathVariable Long id) {
        if (jobRepository.findById(id).isPresent()) {
            jobRepository.findById(id).ifPresent(job -> {
                job.setDeleted(true);
                jobRepository.save(job);
            });
            return ResponseEntity.ok(okResponse("Job disabled successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get analytics dashboard data")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalUsers", userRepository.countByDeletedFalse());
        dashboard.put("totalRecruiters", userRepository.countByDeletedFalseAndRole(UserRole.ROLE_RECRUITER));
        dashboard.put("totalJobs", jobRepository.count());
        dashboard.put("activeJobs", jobRepository.countByStatusAndDeletedFalse(com.jobmatch.domain.enums.JobStatus.ACTIVE));
        dashboard.put("monthlyReports", reportingService.getAllMonthlyReports());
        dashboard.put("weeklyStats", reportingService.getAllWeeklyStats());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get all system settings")
    public ResponseEntity<List<SystemSetting>> getSettings() {
        return ResponseEntity.ok(systemSettingRepository.findAll());
    }

    @PutMapping("/settings/{key}")
    @Operation(summary = "Update a system setting")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        if (systemSettingRepository.findBySettingKey(key).isPresent()) {
            systemSettingRepository.findBySettingKey(key).ifPresent(setting -> {
                setting.setSettingValue(body.get("value"));
                setting.setUpdatedBy(userIdResolver.getCurrentUserId());
                systemSettingRepository.save(setting);
            });
            return ResponseEntity.ok(okResponse("Setting updated"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/reports/daily")
    @Operation(summary = "Get daily report data")
    public ResponseEntity<Map<String, Object>> getDailyReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("monthlyReports", reportingService.getAllMonthlyReports());
        report.put("weeklyStats", reportingService.getAllWeeklyStats());
        return ResponseEntity.ok(report);
    }

    private Map<String, Object> okResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }
}