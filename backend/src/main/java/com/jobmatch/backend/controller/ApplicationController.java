package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.ApplicationResponse;
import com.jobmatch.backend.dto.ApplyRequest;
import com.jobmatch.backend.service.ApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<ApplicationResponse>> myApplications() {
        return ResponseEntity.ok(applicationService.getMyApplications());
    }

    // ✅ Get applications for a job (RECRUITER)
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> applicationsByJob(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId));
    }
}
