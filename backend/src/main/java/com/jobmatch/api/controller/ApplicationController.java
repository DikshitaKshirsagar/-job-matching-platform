package com.jobmatch.api.controller;

import com.jobmatch.api.dto.request.ApplyJobRequest;
import com.jobmatch.api.dto.response.ApiResponse;
import com.jobmatch.api.dto.response.ApplicationResponse;
import com.jobmatch.service.ApplicationService;
import com.jobmatch.util.UserIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Applications", description = "Job application management endpoints")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserIdResolver userIdResolver;

    @PostMapping("/apply")
    @Operation(summary = "Apply to a job", description = "Job seeker applies to a job posting")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyToJob(
            @Valid @RequestBody ApplyJobRequest request) {
        log.info("Apply to job id: {}", request.getJobId());
        Long applicantId = userIdResolver.getCurrentUserId();
        ApplicationResponse response = applicationService.applyToJob(request, applicantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", response));
    }

    @GetMapping("/my")
    @Operation(summary = "Get user's applications", description = "Retrieve all applications submitted by the current user")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications() {
        log.debug("Get user's applications");
        Long userId = userIdResolver.getCurrentUserId();
        List<ApplicationResponse> applications = applicationService.getMyApplicationsList(userId);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @GetMapping("/my-paginated")
    @Operation(summary = "Get user's applications (paginated)", description = "Retrieve applications with pagination")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getMyApplicationsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Get user's applications paginated: page={}, size={}", page, size);
        Long userId = userIdResolver.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<ApplicationResponse> applications = applicationService.getMyApplications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get job applicants", description = "Recruiter views all applications for their job")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getJobApplications(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Get applications for job id: {}", jobId);
        Long recruiterId = userIdResolver.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<ApplicationResponse> applications = applicationService.getApplicationsByJob(jobId, recruiterId, pageable);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @PatchMapping("/{applicationId}/status")
    @Operation(summary = "Update application status", description = "Recruiter updates the status of an application")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam String status) {
        log.info("Update application id: {} status to: {}", applicationId, status);
        Long recruiterId = userIdResolver.getCurrentUserId();
        ApplicationResponse response = applicationService.updateApplicationStatus(applicationId, status, recruiterId);
        return ResponseEntity.ok(ApiResponse.success("Application status updated", response));
    }

}
