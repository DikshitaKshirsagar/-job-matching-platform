package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.AuthResponse;
import com.jobmatch.backend.dto.ForgotPasswordRequest;
import com.jobmatch.backend.dto.LoginRequest;
import com.jobmatch.backend.dto.RegisterRequest;
import com.jobmatch.backend.dto.ResetPasswordRequest;
import com.jobmatch.backend.dto.VerifyEmailRequest;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.UserRepository;
import com.jobmatch.backend.security.JwtUtil;
import com.jobmatch.backend.service.EmailService;
import com.jobmatch.backend.service.AuthServiceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    // REGISTER USER
    public AuthResponse register(RegisterRequest request) {

        // Input validation
        validateRegisterRequest(request);

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        // Create new user (unverified)
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Ignore profileImage if sent (no field in User yet)

        // Set role safely (default to SEEKER)
        Role role;
        try {
            role = (request.getRole() != null) ? Role.valueOf(request.getRole().toUpperCase()) : Role.SEEKER;
        } catch (IllegalArgumentException e) {
            role = Role.SEEKER;
        }

        user.setRole(role);
        user.setEmailVerified(false);

        // Generate verification token
        String verifToken = AuthServiceHelper.generateVerificationToken();
        user.setVerificationToken(verifToken);
        user.setVerificationTokenExpiry(AuthServiceHelper.getVerificationExpiry());

        // Save user
        User savedUser = userRepository.save(user);

        // Send verification email (mock)
        emailService.sendVerificationEmail(savedUser.getEmail(), verifToken, savedUser.getName());

        // Return message - no JWT yet
        return new AuthResponse(null,
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getId(),
                "Registration successful! Please check your email to verify your account."
        );
    }

    private void validateRegisterRequest(RegisterRequest request) {
        // Email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        if (!request.getEmail().matches(emailRegex)) {
            throw new AppException("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        // Name validation
        if (request.getName() == null || request.getName().trim().length() < 2 || request.getName().trim().length() > 50) {
            throw new AppException("Name must be 2-50 characters", HttpStatus.BAD_REQUEST);
        }

        // Password strength
        String password = request.getPassword();
        if (password.length() < 8 || 
            !password.matches(".*[A-Z].*") || 
            !password.matches(".*[a-z].*") || 
            !password.matches(".*\\d.*")) {
            throw new AppException("Password must be 8+ chars with uppercase, lowercase, and digit", HttpStatus.BAD_REQUEST);
        }
    }

    // LOGIN USER
    // VERIFY EMAIL
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByVerificationToken(request.getToken())
                .orElseThrow(() -> new AppException("Invalid or expired verification token", HttpStatus.BAD_REQUEST));

        if (!AuthServiceHelper.isVerificationTokenValid(user.getVerificationToken(), user.getVerificationTokenExpiry())) {
            throw new AppException("Invalid or expired verification token", HttpStatus.BAD_REQUEST);
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getId(),
                "Email verified successfully! You can now login."
        );
    }

    // FORGOT PASSWORD
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("Email not found", HttpStatus.NOT_FOUND));

        String resetToken = AuthServiceHelper.generateResetToken();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(AuthServiceHelper.getResetExpiry());
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken, user.getName());

        return new AuthResponse(null, null, user.getEmail(), null, null, 
                "Password reset link sent to your email.");
    }

    // RESET PASSWORD
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new AppException("Invalid or expired reset token", HttpStatus.BAD_REQUEST));

        if (!AuthServiceHelper.isResetTokenValid(user.getPasswordResetToken(), user.getPasswordResetTokenExpiry())) {
            throw new AppException("Invalid or expired reset token", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        return new AuthResponse(null, user.getName(), user.getEmail(), user.getRole(), user.getId(),
                "Password reset successful. You can now login with new password.");
    }

    // LOGIN USER
    public AuthResponse login(LoginRequest request) {

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED)
                );

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user);

        // Return response
        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getId(),
                "Login successful"
        );
    }
}
