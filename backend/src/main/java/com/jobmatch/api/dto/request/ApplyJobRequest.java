package com.jobmatch.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyJobRequest {

    @NotNull(message = "Job id is required")
    private Long jobId;

    private String coverLetter;
}
