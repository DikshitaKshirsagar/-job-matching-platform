package com.jobmatch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_reports", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"month_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear;

    @Column(name = "total_jobs", nullable = false)
    private int totalJobs;

    @Column(name = "total_applications", nullable = false)
    private int totalApplications;

    @Column(name = "total_users", nullable = false)
    private int totalUsers;

    @Column(name = "total_recruiters", nullable = false)
    private int totalRecruiters;

    @Column(name = "applications_per_job", nullable = false, precision = 10, scale = 2)
    private BigDecimal applicationsPerJob;

    @Column(name = "hiring_conversion_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal hiringConversionRate;

    @Column(columnDefinition = "TEXT")
    private String topSkills;

    @Column(columnDefinition = "TEXT")
    private String recruiterPerformance;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}