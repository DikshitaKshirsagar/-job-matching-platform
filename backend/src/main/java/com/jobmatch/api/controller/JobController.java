package com.jobmatch.api.controller;

import com.jobmatch.api.dto.request.CreateJobRequest;
import com.jobmatch.api.dto.request.UpdateJobRequest;
import com.jobmatch.api.dto.response.ApiResponse;
import com.jobmatch.api.dto.response.JobResponse;
import com.jobmatch.service.JobService;
import com.jobmatch.util.UserIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jobs", description = "Job listing and management endpoints")
public class JobController {

    private final JobService jobService;
    private final UserIdResolver userIdResolver;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_RECRUITER')")
    @Operation(summary = "Create a new job", description = "Recruiter creates a new job posting")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(@Valid @RequestBody CreateJobRequest request) {
        log.info("Create job request: {}", request.getTitle());
        Long recruiterId = userIdResolver.getCurrentUserId();
        JobResponse response = jobService.createJob(request, recruiterId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Search jobs", description = "Search and list jobs with optional filters")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getAllJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        log.debug("Search jobs: keyword={}, location={}, page={}, size={}", keyword, location, page, size);
        
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(direction), sortBy));
        Page<JobResponse> jobs = jobService.searchJobs(keyword, location, pageable);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID", description = "Retrieve a single job posting")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable Long id) {
        log.debug("Get job by id: {}", id);
        JobResponse job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_RECRUITER')")
    @Operation(summary = "Update job", description = "Recruiter updates their own job posting")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request) {
        log.info("Update job id: {}", id);
        Long recruiterId = userIdResolver.getCurrentUserId();
        JobResponse response = jobService.updateJob(id, request, recruiterId);
        return ResponseEntity.ok(ApiResponse.success("Job updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_RECRUITER')")
    @Operation(summary = "Delete job", description = "Recruiter deletes their own job posting")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        log.info("Delete job id: {}", id);
        Long recruiterId = userIdResolver.getCurrentUserId();
        jobService.deleteJob(id, recruiterId);
        return ResponseEntity.ok(ApiResponse.successMessage("Job deleted successfully"));
    }

    @GetMapping("/recruiter/my-jobs")
    @PreAuthorize("hasRole('ROLE_RECRUITER')")
    @Operation(summary = "Get recruiter's jobs", description = "List all job postings by the current recruiter")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getMyJobs() {
        log.debug("Get recruiter's jobs");
        Long recruiterId = userIdResolver.getCurrentUserId();
        List<JobResponse> jobs = jobService.getJobsByRecruiter(recruiterId);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

}
