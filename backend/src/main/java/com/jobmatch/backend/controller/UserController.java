package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.DashboardResponse;
import com.jobmatch.backend.dto.ErrorResponse;
import com.jobmatch.backend.dto.ResumeUploadResponse;
import com.jobmatch.backend.dto.UserProfileResponse;
import com.jobmatch.backend.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    // ✅ GET DASHBOARD
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            DashboardResponse response = userService.getDashboard();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ✅ GET PROFILE
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            UserProfileResponse response = userService.getUserProfile();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ✅ UPLOAD RESUME
    @PostMapping("/upload-resume")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            ResumeUploadResponse response = userService.uploadResume(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // 🔥🔥 TEMPORARY DEBUG API (VERY IMPORTANT)
    @GetMapping("/debug")
    public ResponseEntity<?> debug() {
        try {
            String user = SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
