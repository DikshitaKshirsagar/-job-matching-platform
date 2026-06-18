package com.jobmatch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_stats", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"week_start"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WeeklyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "new_jobs", nullable = false)
    private int newJobs;

    @Column(name = "new_applications", nullable = false)
    private int newApplications;

    @Column(name = "new_users", nullable = false)
    private int newUsers;

    @Column(name = "new_recruiters", nullable = false)
    private int newRecruiters;

    @Column(name = "active_jobs", nullable = false)
    private int activeJobs;

    @Column(name = "total_applications", nullable = false)
    private int totalApplications;

    @Column(name = "hired_count", nullable = false)
    private int hiredCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}