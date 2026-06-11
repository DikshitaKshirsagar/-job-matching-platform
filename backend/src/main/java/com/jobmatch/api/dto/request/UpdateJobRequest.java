package com.jobmatch.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateJobRequest {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(min = 50, message = "Description must be at least 50 characters")
    private String description;

    private String company;
    private String location;
    private String jobType;

    @Min(value = 0, message = "Minimum salary cannot be negative")
    private BigDecimal salaryMin;

    @Min(value = 0, message = "Maximum salary cannot be negative")
    private BigDecimal salaryMax;

    private String requiredSkills;
    private String experienceLevel;
}
