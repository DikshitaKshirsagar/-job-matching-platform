package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.ApplicationResponse;
import com.jobmatch.backend.dto.ApplicationStatusUpdateRequest;
import com.jobmatch.backend.dto.ApplyRequest;
import com.jobmatch.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationService applicationService;

    // ✅ Apply to job
    @PostMapping("/apply")
    public ResponseEntity<ApplicationResponse> apply(@Valid @RequestBody ApplyRequest request) {
        return ResponseEntity.ok(applicationService.applyToJob(request));
    }

    // ✅ Get my applications (SEEKER)
    @GetMapping("/my")
    public ResponseEntity<Page<ApplicationResponse>> myApplications(Pageable pageable) {
        return ResponseEntity.ok(applicationService.getMyApplications(pageable));
    }

    // ✅ Get applications for a job (RECRUITER)
    @GetMapping("/job/{jobId}")
    public ResponseEntity<Page<ApplicationResponse>> applicationsByJob(
            @PathVariable Long jobId,
            Pageable pageable) {

        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId, pageable));
    }

    // ✅ Update application status (RECRUITER)
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(applicationId, request));
    }
}
