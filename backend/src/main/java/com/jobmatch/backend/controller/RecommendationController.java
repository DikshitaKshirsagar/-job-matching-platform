package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.JobListResponse;
import com.jobmatch.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<List<JobListResponse>> recommendJobs() {
        return ResponseEntity.ok(jobService.getJobRecommendations());
    }
}
