package com.jobmatch.service.impl;

import com.jobmatch.api.dto.request.LoginRequest;
import com.jobmatch.api.dto.request.RegisterRequest;
import com.jobmatch.api.dto.response.AuthResponse;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ConflictException;
import com.jobmatch.exception.ResourceNotFoundException;
import com.jobmatch.exception.UnauthorizedException;
import com.jobmatch.infrastructure.security.JwtTokenProvider;
import com.jobmatch.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (request == null) {
            throw new BadRequestException("Registration request cannot be null");
        }

        String name = normalizeRequired(request.getFullName(), "Full name is required");
        String email = normalizeEmail(request.getEmail());
        String password = normalizeRequired(request.getPassword(), "Password is required");
        String confirmPassword = request.getConfirmPassword();
        String rawRole = normalizeRequired(request.getRole(), "Account type is required");

        if (confirmPassword != null && !confirmPassword.isBlank()) {
            confirmPassword = confirmPassword.trim();
            if (!password.equals(confirmPassword)) {
                log.warn("Registration failed: password and confirmPassword do not match for email: {}", email);
                throw new BadRequestException("Password and confirm password must match");
            }
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed: Email already exists: {}", email);
            throw new ConflictException("Email already exists. Please sign in instead.");
        }

        UserRole role = resolveRole(rawRole);

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEmailVerified(true);

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with id: {}", savedUser.getId());

        String token = null;
        try {
            token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());
        } catch (RuntimeException ex) {
            log.warn("User registered, but registration token generation failed: {}", ex.getMessage());
        }

        return AuthResponse.builder()
                .token(token)
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .userId(savedUser.getId())
                .message("Registration successful")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        if (request == null) {
            throw new BadRequestException("Login request cannot be null");
        }

        String email = normalizeEmail(request.getEmail());
        String password = normalizeRequired(request.getPassword(), "Password is required");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found with email: {}", email);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed: Invalid password for email: {}", email);
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());
        log.info("Successfully logged in user with id: {}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .userId(user.getId())
                .message("Login successful")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse saveResume(String email, String resumeText) {
        log.info("Saving resume for email: {}", email);

        String normalizedEmail = normalizeEmail(email);
        String cleanedResumeText = normalizeRequired(resumeText, "Resume text is required");

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Resume save failed: User not found with email: {}", normalizedEmail);
                    return new ResourceNotFoundException("User", "email", normalizedEmail);
                });

        user.setResumeText(cleanedResumeText);
        userRepository.save(user);
        log.info("Successfully saved resume for user id: {}", user.getId());

        return AuthResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .userId(user.getId())
                .message("Resume uploaded successfully")
                .build();
    }

    private UserRole resolveRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return UserRole.ROLE_JOB_SEEKER;
        }

        try {
            String normalizedRole = rawRole.trim().toUpperCase();
            normalizedRole = normalizedRole.replaceAll("[^A-Z0-9]+", "_");
            if ("SEEKER".equals(normalizedRole) || "JOB-SEEKER".equals(normalizedRole)) {
                normalizedRole = "JOB_SEEKER";
            }
            if ("RECRUITER".equals(normalizedRole)) {
                normalizedRole = "RECRUITER";
            }
            if ("ADMIN".equals(normalizedRole)) {
                normalizedRole = "ADMIN";
            }
            if (!normalizedRole.startsWith("ROLE_")) {
                normalizedRole = "ROLE_" + normalizedRole;
            }
            return UserRole.valueOf(normalizedRole);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + rawRole);
        }
    }

    private String normalizeEmail(String email) {
        return normalizeRequired(email, "Email is required").toLowerCase();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException(message);
        }

        return normalized;
    }
}
