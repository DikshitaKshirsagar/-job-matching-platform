package com.jobmatch.api.dto.response;

import com.jobmatch.domain.enums.JobStatus;
import com.jobmatch.domain.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private String title;
    private String company;
    private String description;
    private List<String> requiredSkills;
    private String location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private JobType jobType;
    private JobStatus status;
    private Long recruiterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
