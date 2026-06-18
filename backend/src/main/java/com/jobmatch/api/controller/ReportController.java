package com.jobmatch.api.controller;

import com.jobmatch.domain.entity.MonthlyReport;
import com.jobmatch.domain.entity.WeeklyStats;
import com.jobmatch.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Reporting and export endpoints")
public class ReportController {

    private final ReportingService reportingService;

    @GetMapping("/monthly")
    @Operation(summary = "Get all monthly reports")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RECRUITER')")
    public ResponseEntity<List<MonthlyReport>> getMonthlyReports() {
        return ResponseEntity.ok(reportingService.getAllMonthlyReports());
    }

    @GetMapping("/monthly/{monthYear}")
    @Operation(summary = "Get monthly report by month-year")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RECRUITER')")
    public ResponseEntity<MonthlyReport> getMonthlyReport(@PathVariable String monthYear) {
        MonthlyReport report = reportingService.getMonthlyReport(monthYear);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/weekly")
    @Operation(summary = "Get all weekly statistics")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RECRUITER')")
    public ResponseEntity<List<WeeklyStats>> getWeeklyStats() {
        return ResponseEntity.ok(reportingService.getAllWeeklyStats());
    }
}