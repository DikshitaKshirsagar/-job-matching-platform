package com.jobmatch.api.controller;

import com.jobmatch.api.dto.response.ApiResponse;
import com.jobmatch.api.dto.response.UserResponse;
import com.jobmatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User profile and settings endpoints")
public class UserController {

    private final UserService userService;

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
        Long userId = getCurrentUserId();
        UserResponse updated = userService.updateUserProfile(userId, name);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping("/upload-resume")
    @Operation(summary = "Upload resume", description = "Upload a PDF resume file")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadResume(
            @RequestParam("file") MultipartFile file) {
        log.info("Upload resume");
        Long userId = getCurrentUserId();
        userService.uploadResume(userId, file);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Resume uploaded successfully");
        response.put("fileName", file.getOriginalFilename());
        
        return ResponseEntity.ok(ApiResponse.success("Resume uploaded successfully", response));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard data", description = "Get user dashboard with summary statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        log.debug("Get dashboard data");
        Long userId = getCurrentUserId();
        UserResponse profile = userService.getUserProfile(userId);
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("user", profile);
        dashboard.put("applicationsCount", 0); // To be populated from ApplicationService
        dashboard.put("savedJobsCount", 0);     // To be populated from SavedJobService
        
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // In production, extract from JWT token claims
        // For now, return a placeholder
        return 1L;
    }
}
