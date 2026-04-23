package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.AuthResponse;
import com.jobmatch.backend.dto.ErrorResponse;
import com.jobmatch.backend.dto.LoginRequest;
import com.jobmatch.backend.dto.RegisterRequest;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/resume")
    public ResponseEntity<?> uploadResume(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Please login to upload resume"));
            }

            String email = authentication.getName();
            String resumeText = request != null ? request.get("resumeText") : null;

            AuthResponse response = authService.saveResume(email, resumeText);
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            HttpStatus status = Objects.requireNonNull(e.getStatus(), "AppException status must not be null");
            return ResponseEntity.status(status)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An unexpected error occurred"));
        }
    }
}
