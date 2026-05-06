package com.jobmatch.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record JobListResponse(
        Long id,
        String title,
        String company,
        String description,
        String skills,
        String location,
        String salary,
        Long recruiterId,
        LocalDateTime createdAt,
        Double matchScore,
        List<String> skillsMatched,
        List<String> skillsMissing,
        Double recommendationScore
) {
}
