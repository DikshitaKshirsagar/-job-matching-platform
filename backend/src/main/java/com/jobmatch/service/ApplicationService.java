package com.jobmatch.service;

import com.jobmatch.api.dto.request.ApplyJobRequest;
import com.jobmatch.api.dto.response.ApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicationService {
    ApplicationResponse applyToJob(ApplyJobRequest request, Long applicantId);
    Page<ApplicationResponse> getMyApplications(Long userId, Pageable pageable);
    List<ApplicationResponse> getMyApplicationsList(Long userId);
    Page<ApplicationResponse> getApplicationsByJob(Long jobId, Pageable pageable);
    ApplicationResponse updateApplicationStatus(Long applicationId, String status, Long recruiterId);
}
