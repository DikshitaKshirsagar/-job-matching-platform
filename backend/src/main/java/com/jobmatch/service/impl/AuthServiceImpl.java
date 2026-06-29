package com.jobmatch.service.impl;

import com.jobmatch.api.dto.request.ForgotPasswordRequest;
import com.jobmatch.api.dto.request.LoginRequest;
import com.jobmatch.api.dto.request.RegisterRequest;
import com.jobmatch.api.dto.request.ResetPasswordRequest;
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
import com.jobmatch.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be null");
        }

        log.info("Registering new user with email: {}", request.getEmail());

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

        // Generate email verification token (not auto-verified anymore)
        //String verificationToken = UUID.randomUUID().toString();
        //user.setVerificationToken(verificationToken);
        //user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        //user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with id: {}", savedUser.getId());

        // Send verification email
        //emailService.sendVerificationEmail(email, verificationToken);

        String token = null;
        String refreshToken = null;
        try {
            token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());
            refreshToken = jwtTokenProvider.generateRefreshTokenFromEmail(user.getEmail());
        } catch (RuntimeException ex) {
            log.warn("User registered, but token generation failed: {}", ex.getMessage());
        }

        // Save refresh token to user entity
        if (refreshToken != null) {
            savedUser.setRefreshToken(refreshToken);
            userRepository.save(savedUser);
        }

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .userId(savedUser.getId())
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("Login request cannot be null");
        }

        log.info("Login attempt for email: {}", request.getEmail());

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

        // Optionally block login until email is verified
       // if (!user.isEmailVerified()) {
         //   log.warn("Login failed: Email not verified for user: {}", email);
           // throw new UnauthorizedException("Please verify your email before logging in. Check your inbox for the verification link.");
        //}

        String token = jwtTokenProvider.generateTokenFromEmail(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshTokenFromEmail(user.getEmail());

        // Save refresh token to user entity
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        log.info("Successfully logged in user with id: {}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
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

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        log.info("Processing token refresh request");

        // Validate the refresh token
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("Invalid or expired refresh token");
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Refresh failed: User not found with email: {}", email);
                    return new UnauthorizedException("Invalid or expired refresh token");
                });

        // Verify the stored refresh token matches
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            log.warn("Refresh token mismatch for user: {}", email);
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Generate new access token
        String newToken = jwtTokenProvider.generateTokenFromEmail(user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshTokenFromEmail(user.getEmail());

        // Rotate the refresh token
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        log.info("Successfully refreshed token for user id: {}", user.getId());

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .userId(user.getId())
                .message("Token refreshed successfully")
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Verification token is required");
        }

        log.info("Processing email verification with token");

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> {
                    log.warn("Email verification failed: Invalid token");
                    return new BadRequestException("Invalid or expired verification token");
                });

        if (user.isEmailVerified()) {
            log.warn("Email already verified for user id: {}", user.getId());
            return; // Already verified, no-op
        }

        if (user.getVerificationTokenExpiry() != null && user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Email verification failed: Token expired for user id: {}", user.getId());
            throw new BadRequestException("Verification token has expired. Please register again.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Successfully verified email for user id: {}", user.getId());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = normalizeEmail(request.getEmail());
        log.info("Processing forgot password request for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Don't reveal if email exists or not for security
        if (user == null) {
            log.warn("Forgot password: No user found with email: {}", email);
            return;
        }

        // Generate password reset token (1 hour expiry)
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Send password reset email
        emailService.sendPasswordResetEmail(email, resetToken);

        log.info("Password reset token generated for user id: {}", user.getId());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new BadRequestException("Reset password request cannot be null");
        }

        String token = normalizeRequired(request.getToken(), "Token is required");
        String newPassword = normalizeRequired(request.getNewPassword(), "New password is required");

        log.info("Processing password reset with token");

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> {
                    log.warn("Password reset failed: Invalid token");
                    return new BadRequestException("Invalid or expired password reset token");
                });

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed: Token expired for user id: {}", user.getId());
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        // Set new password and clear reset token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Successfully reset password for user id: {}", user.getId());
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