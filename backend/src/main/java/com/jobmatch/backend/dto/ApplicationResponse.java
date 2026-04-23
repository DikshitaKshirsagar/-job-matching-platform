package com.jobmatch.backend.dto;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String company,
        String location,
        String status,
        LocalDateTime appliedAt
) {
}
