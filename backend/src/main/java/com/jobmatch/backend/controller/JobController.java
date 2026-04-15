package com.jobmatch.backend.controller;

import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    // POST /api/v1/jobs — Recruiter only
    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long recruiterId = (Long) request.getAttribute("userId"); // your JWT filter sets this
        Job created = jobService.createJob(job, role, recruiterId);
        return ResponseEntity.status(201).body(created);
    }

    // GET /api/v1/jobs — Any logged-in user
    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    // GET /api/v1/jobs/my-jobs — Recruiter only (MUST be above /{id})
    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getMyJobs(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long recruiterId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(jobService.getMyJobs(role, recruiterId));
    }

    // GET /api/v1/jobs/{id} — Any logged-in user
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }
}