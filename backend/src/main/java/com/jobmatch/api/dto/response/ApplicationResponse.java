package com.jobmatch.api.dto.response;

import com.jobmatch.domain.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private Double matchScore;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private String applicantName;
    private String applicantEmail;
}
