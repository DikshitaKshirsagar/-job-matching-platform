package com.jobmatch.backend.controller;

import com.jobmatch.backend.dto.*;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> uploadResume(@RequestBody Map<String, String> request,
                                          Authentication authentication) {

        try {
            if (authentication == null || authentication.getName() == null) {
                throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            String email = authentication.getName();
            String resumeText = request.get("resumeText");

            return ResponseEntity.ok(authService.saveResume(email, resumeText));

        } catch (AppException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
