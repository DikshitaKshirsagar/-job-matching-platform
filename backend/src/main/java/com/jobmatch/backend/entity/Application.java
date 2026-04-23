package com.jobmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Many applications -> one user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // ✅ Many applications -> one job
    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    // ✅ AI match score
    private Double matchScore;

    // ✅ Application status
    private String status;

    // ✅ Timestamp
    private LocalDateTime appliedAt;

    // ✅ Auto set time before saving
    @PrePersist
    public void prePersist() {
        this.appliedAt = LocalDateTime.now();
    }
}