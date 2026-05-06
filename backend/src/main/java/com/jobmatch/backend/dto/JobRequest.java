package com.jobmatch.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record JobRequest(
        @NotBlank(message = "Job title is required")
        String title,

        @NotBlank(message = "Company is required")
        String company,

        @NotBlank(message = "Job description is required")
        String description,

        String location,

        String salary,

        String skills
) {
}
