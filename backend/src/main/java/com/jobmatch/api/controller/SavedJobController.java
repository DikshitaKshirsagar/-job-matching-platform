package com.jobmatch.api.controller;

import com.jobmatch.api.dto.response.ApiResponse;
import com.jobmatch.api.dto.response.SavedJobResponse;
import com.jobmatch.service.SavedJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/saved-jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Saved Jobs", description = "Save and manage job bookmarks")
public class SavedJobController {

    private final SavedJobService savedJobService;

    @PostMapping("/{jobId}")
    @Operation(summary = "Save job", description = "Add a job to the user's saved jobs list")
    public ResponseEntity<ApiResponse<SavedJobResponse>> saveJob(@PathVariable Long jobId) {
        log.info("Save job id: {}", jobId);
        Long userId = getCurrentUserId();
        SavedJobResponse response = savedJobService.saveJob(jobId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job saved successfully", response));
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Unsave job", description = "Remove a job from the user's saved jobs list")
    public ResponseEntity<ApiResponse<Void>> unsaveJob(@PathVariable Long jobId) {
        log.info("Unsave job id: {}", jobId);
        Long userId = getCurrentUserId();
        savedJobService.unsaveJob(jobId, userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Job removed from saved"));
    }

    @GetMapping
    @Operation(summary = "Get saved jobs", description = "Retrieve all saved jobs for the current user")
    public ResponseEntity<ApiResponse<List<SavedJobResponse>>> getSavedJobs() {
        log.debug("Get saved jobs");
        Long userId = getCurrentUserId();
        List<SavedJobResponse> savedJobs = savedJobService.getSavedJobs(userId);
        return ResponseEntity.ok(ApiResponse.success(savedJobs));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // In production, extract from JWT token claims
        // For now, return a placeholder
        return 1L;
    }
}
