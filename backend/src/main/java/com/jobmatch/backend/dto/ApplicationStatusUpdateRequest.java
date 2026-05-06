package com.jobmatch.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplicationStatusUpdateRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
