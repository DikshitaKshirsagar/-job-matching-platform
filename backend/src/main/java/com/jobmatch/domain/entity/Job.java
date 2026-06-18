package com.jobmatch.domain.entity;

import com.jobmatch.domain.enums.JobStatus;
import com.jobmatch.domain.enums.JobType;
import com.jobmatch.infrastructure.persistence.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_recruiter_id", columnList = "recruiter_id"),
    @Index(name = "idx_job_recruiter_status", columnList = "recruiter_id,status"),
    @Index(name = "idx_job_active_listing", columnList = "status,is_deleted,created_at"),
    @Index(name = "idx_job_status_location", columnList = "status,location"),
    @Index(name = "idx_job_created_at", columnList = "created_at"),
    @Index(name = "idx_job_location", columnList = "location")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @Convert(converter = StringListConverter.class)
    @Column(name = "required_skills", columnDefinition = "JSON")
    private List<String> requiredSkills = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<JobSkill> jobSkills = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}