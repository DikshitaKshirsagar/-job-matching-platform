package com.jobmatch.backend.dto;

import java.time.LocalDateTime;

public record JobListResponse(
        Long id,
        String title,
        String company,
        String description,
        String location,
        String salary,
        Long recruiterId,
        LocalDateTime createdAt,
        Double matchScore
) {
}
