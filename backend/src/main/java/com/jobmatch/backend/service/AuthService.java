package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.AuthResponse;
import com.jobmatch.backend.dto.LoginRequest;
import com.jobmatch.backend.dto.RegisterRequest;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.UserRepository;
import com.jobmatch.backend.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        String name = normalizeRequired(request.getName(), "Name is required");
        String email = normalizeEmail(request.getEmail());
        String password = normalizeRequired(request.getPassword(), "Password is required");

        if (userRepository.existsByEmail(email)) {
            throw new AppException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(resolveRole(request.getRole()));
        user.setEmailVerified(true);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getId(),
                "Registration successful"
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        String password = normalizeRequired(request.getPassword(), "Password is required");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getId(),
                "Login successful"
        );
    }

    public AuthResponse saveResume(String email, String resumeText) {
        String normalizedEmail = normalizeEmail(email);
        String cleanedResumeText = normalizeRequired(resumeText, "Resume text is required");

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setResumeText(cleanedResumeText);
        userRepository.save(user);

        return new AuthResponse(
                null,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getId(),
                "Resume uploaded successfully"
        );
    }

    private Role resolveRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return Role.SEEKER;
        }

        try {
            return Role.valueOf(rawRole.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException("Invalid role", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeEmail(String email) {
        return normalizeRequired(email, "Email is required").toLowerCase();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }

        return normalized;
    }
}
