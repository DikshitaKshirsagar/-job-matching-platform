package com.jobmatch.backend.dto;

import jakarta.validation.constraints.NotNull;

public class ApplyRequest {

    @NotNull(message = "Job id is required")
    private Long jobId;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
