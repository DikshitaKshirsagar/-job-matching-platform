package com.jobmatch.api.controller;

import com.jobmatch.api.dto.response.ApiResponse;
import com.jobmatch.api.dto.response.DashboardStatsResponse;
import com.jobmatch.api.dto.response.JobRecommendationResponse;
import com.jobmatch.api.dto.response.UserResponse;
import com.jobmatch.service.UserService;
import com.jobmatch.util.UserIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User profile and settings endpoints")
public class UserController {

    private final UserService userService;
    private final UserIdResolver userIdResolver;

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Retrieve the current user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile() {
        log.debug("Get current user profile");
        UserResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update the current user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(@RequestBody Map<String, String> request) {
        log.info("Update user profile");
        String name = request.get("name");
        Long userId = userIdResolver.getCurrentUserId();
        UserResponse updated = userService.updateUserProfile(userId, name);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping("/upload-resume")
    @Operation(summary = "Upload resume", description = "Upload a PDF resume file")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadResume(
            @RequestParam("file") MultipartFile file) {
        log.info("Upload resume");
        Long userId = userIdResolver.getCurrentUserId();
        userService.uploadResume(userId, file);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Resume uploaded successfully");
        response.put("fileName", file.getOriginalFilename());
        
        return ResponseEntity.ok(ApiResponse.success("Resume uploaded successfully", response));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard data", description = "Get user dashboard with summary statistics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard() {
        log.debug("Get dashboard data");
        Long userId = userIdResolver.getCurrentUserId();
        DashboardStatsResponse dashboard = userService.getDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get AI job recommendations", description = "Get personalized AI-based job recommendations for the current user")
    public ResponseEntity<ApiResponse<List<JobRecommendationResponse>>> getJobRecommendations() {
        log.info("Get AI job recommendations");
        Long userId = userIdResolver.getCurrentUserId();
        List<JobRecommendationResponse> recommendations = userService.getJobRecommendations(userId);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }

}
