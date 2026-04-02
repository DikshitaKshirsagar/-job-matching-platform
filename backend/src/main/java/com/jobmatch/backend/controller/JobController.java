package com.jobmatch.backend.controller;

import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3008"})
public class JobController {

    private final JobRepository jobRepository;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        List<Job> jobs = jobRepository.findAll();
        return ResponseEntity.ok(jobs);
    }

    @PostConstruct
    public void seedData() {
        long count = jobRepository.count();
        if (count == 0) {
            Job job1 = new Job(null, "Senior Software Engineer", "TechCorp", "Fullstack developer with Java, React, Spring Boot experience. Job matching platform development.", "New York, NY", "$140k - $160k", 1L, null);
            Job job2 = new Job(null, "Frontend Developer", "StartupX", "React.js specialist for dashboard and auth flows.", "Remote", "$100k - $120k", 2L, null);
            jobRepository.saveAll(List.of(job1, job2));
        }
    }
}
