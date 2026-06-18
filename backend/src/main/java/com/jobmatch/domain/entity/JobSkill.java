package com.jobmatch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_skills", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"job_id", "skill_id"})
}, indexes = {
    @Index(name = "idx_job_skills_job", columnList = "job_id"),
    @Index(name = "idx_job_skills_skill", columnList = "skill_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
}