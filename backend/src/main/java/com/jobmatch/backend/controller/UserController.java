package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.DashboardResponse;
import com.jobmatch.backend.dto.ResumeUploadResponse;
import com.jobmatch.backend.dto.UserProfileResponse;
import com.jobmatch.backend.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    // ✅ Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(userService.getDashboard());
    }

    // ✅ Profile
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    // ✅ Upload Resume
    @PostMapping("/upload-resume")
    public ResponseEntity<ResumeUploadResponse> uploadResume(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(userService.uploadResume(file));
    }

    // ✅ Debug (optional)
    @GetMapping("/debug")
    public ResponseEntity<String> debug(Authentication authentication) {
        String user = (authentication != null) ? authentication.getName() : "anonymousUser";
        return ResponseEntity.ok(user);
    }
}