package com.jobmatch.backend.dto;

import java.time.LocalDateTime;

public record JobResponse(
        Long id,
        String title,
        String company,
        String description,
        String skills,
        String location,
        String salary,
        Long recruiterId,
        LocalDateTime createdAt
) {
}
