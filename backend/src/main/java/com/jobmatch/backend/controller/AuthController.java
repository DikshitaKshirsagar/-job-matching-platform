package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.*;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            // ✅ FIXED: use the actual HTTP status from AppException, not always BAD_REQUEST
            return ResponseEntity.status(e.getStatus())
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            // ✅ FIXED: use the actual HTTP status from AppException, not always UNAUTHORIZED
            return ResponseEntity.status(e.getStatus())
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }

    // ✅ UPLOAD RESUME (TEXT)
    @PostMapping("/resume")
    public ResponseEntity<?> uploadResume(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal().equals("anonymousUser")) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Please login to upload resume"));
            }

            String email = authentication.getName();
            String resumeText = request.get("resumeText");

            AuthResponse response = authService.saveResume(email, resumeText);

            return ResponseEntity.ok(response);

        } catch (AppException e) {
            // ✅ FIXED: propagate actual status
            return ResponseEntity.status(e.getStatus())
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}