package com.jobmatch.backend.dto;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String company,
        String location,
        Double matchScore,
        String status,
        LocalDateTime appliedAt,
        String applicantName,
        String applicantEmail
) {
}
