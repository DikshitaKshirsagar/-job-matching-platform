package com.jobmatch.api.dto.response;

import com.jobmatch.domain.enums.JobType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRecommendationResponse {

    private Long id;
    private String title;
    private String description;
    private String company;
    private String location;
    private JobType jobType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private List<String> requiredSkills;
    private LocalDateTime createdAt;
    private double matchScore;

    public JobRecommendationResponse(JobResponse job, double matchScore) {
        this.id = job.getId();
        this.title = job.getTitle();
        this.description = job.getDescription();
        this.company = job.getCompany();
        this.location = job.getLocation();
        this.jobType = job.getJobType();
        this.salaryMin = job.getSalaryMin();
        this.salaryMax = job.getSalaryMax();
        this.requiredSkills = job.getRequiredSkills();
        this.createdAt = job.getCreatedAt();
        this.matchScore = matchScore;
    }
}