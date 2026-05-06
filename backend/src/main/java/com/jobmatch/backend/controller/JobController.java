package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.ApplicationResponse;
import com.jobmatch.backend.dto.JobListResponse;
import com.jobmatch.backend.dto.JobRequest;
import com.jobmatch.backend.dto.JobResponse;
import com.jobmatch.backend.service.ApplicationService;
import com.jobmatch.backend.service.JobService;
import com.jobmatch.backend.service.SavedJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private SavedJobService savedJobService;

    // POST /api/v1/jobs — Recruiter only
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest job, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long recruiterId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(201).body(jobService.createJob(job, role, recruiterId));
    }

    // GET /api/v1/jobs — Any logged-in user with optional filters
    @GetMapping
    public ResponseEntity<Page<JobListResponse>> getAllJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return ResponseEntity.ok(jobService.getJobs(location, salary, skills, query, pageable));
    }

    // POST /api/v1/jobs/{jobId}/save — Save a job for the current user
    @PostMapping("/{jobId}/save")
    public ResponseEntity<?> saveJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(savedJobService.saveJob(jobId));
    }

    // DELETE /api/v1/jobs/{jobId}/save — Remove a saved job
    @DeleteMapping("/{jobId}/save")
    public ResponseEntity<Void> unsaveJob(@PathVariable Long jobId) {
        savedJobService.unsaveJob(jobId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/jobs/my-jobs — Recruiter only
    @GetMapping("/my-jobs")
    public ResponseEntity<Page<JobResponse>> getMyJobs(HttpServletRequest request, Pageable pageable) {
        String role = (String) request.getAttribute("role");
        Long recruiterId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(jobService.getMyJobs(role, recruiterId, pageable));
    }

    // GET /api/v1/jobs/{id} — Any logged-in user
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // ✅ GET /api/v1/jobs/{jobId}/applicants — Recruiter only
    @GetMapping("/{jobId}/applicants")
    public ResponseEntity<Page<ApplicationResponse>> getApplicants(
            @PathVariable Long jobId,
            Pageable pageable) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId, pageable));
    }
}
