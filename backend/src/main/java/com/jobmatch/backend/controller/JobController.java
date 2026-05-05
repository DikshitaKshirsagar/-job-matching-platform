package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.ApplicationResponse;
import com.jobmatch.backend.dto.JobListResponse;
import com.jobmatch.backend.dto.JobRequest;
import com.jobmatch.backend.dto.JobResponse;
import com.jobmatch.backend.service.ApplicationService;
import com.jobmatch.backend.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService; // ← ADD THIS

    // POST /api/v1/jobs — Recruiter only
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest job, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long recruiterId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(201).body(jobService.createJob(job, role, recruiterId));
    }
    // GET /api/v1/jobs — Any logged-in user
    @GetMapping
    public ResponseEntity<List<JobListResponse>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    // GET /api/v1/jobs/my-jobs — Recruiter only (MUST be above /{id})
    @GetMapping("/my-jobs")
    public ResponseEntity<List<JobResponse>> getMyJobs(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long recruiterId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(jobService.getMyJobs(role, recruiterId));
    }

    // GET /api/v1/jobs/{id} — Any logged-in user
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // ✅ GET /api/v1/jobs/{jobId}/applicants — Recruiter only (NEW - was missing!)
    @GetMapping("/{jobId}/applicants")
    public ResponseEntity<List<ApplicationResponse>> getApplicants(
            @PathVariable Long jobId) {
        List<ApplicationResponse> applicants = applicationService.getApplicationsByJob(jobId);
        return ResponseEntity.ok(applicants);
    }
}
