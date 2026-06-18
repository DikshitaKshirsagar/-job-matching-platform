package com.jobmatch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_jobs", indexes = {
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_type", columnList = "job_type"),
    @Index(name = "idx_batch_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "processed_items", nullable = false)
    private int processedItems;

    @Column(name = "failed_items", nullable = false)
    private int failedItems;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}