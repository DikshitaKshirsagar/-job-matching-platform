package com.jobmatch.service;

import com.jobmatch.domain.entity.MonthlyReport;
import com.jobmatch.domain.entity.WeeklyStats;
import com.jobmatch.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final MonthlyReportRepository monthlyReportRepository;
    private final WeeklyStatsRepository weeklyStatsRepository;

    @Transactional(readOnly = true)
    public List<MonthlyReport> getAllMonthlyReports() {
        return monthlyReportRepository.findAll();
    }

    @Transactional(readOnly = true)
    public MonthlyReport getMonthlyReport(String monthYear) {
        return monthlyReportRepository.findByMonthYear(monthYear).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<WeeklyStats> getAllWeeklyStats() {
        return weeklyStatsRepository.findAll();
    }
}