package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.*;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.UserRepository;
import com.jobmatch.backend.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    // ✅ REMOVED unused EmailService — was causing potential bean startup failure

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        validateRegisterRequest(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role;
        try {
            role = (request.getRole() != null)
                    ? Role.valueOf(request.getRole().toUpperCase())
                    : Role.SEEKER;
        } catch (Exception e) {
            role = Role.SEEKER;
        }

        user.setRole(role);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        User savedUser = userRepository.save(user);

        System.out.println("USER SAVED: " + savedUser.getEmail());

        String token = jwtUtil.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getId(),
                "Registration successful! You can now login."
        );
    }

    private void validateRegisterRequest(RegisterRequest request) {

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new AppException("Email is required", HttpStatus.BAD_REQUEST);
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        if (!request.getEmail().matches(emailRegex)) {
            throw new AppException("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        if (request.getName() == null || request.getName().trim().length() < 2) {
            throw new AppException("Name must be at least 2 characters", HttpStatus.BAD_REQUEST);
        }

        String password = request.getPassword();
        if (password == null || password.length() < 8 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*\\d.*")) {

            throw new AppException(
                    "Password must be 8+ chars with uppercase, lowercase, and digit",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED)
                );

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isEmailVerified()) {
            throw new AppException("Please verify your email before logging in.", HttpStatus.UNAUTHORIZED);
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

    @Transactional
    public AuthResponse saveResume(String email, String resumeText) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new AppException("Resume file is empty", HttpStatus.BAD_REQUEST);
        }

        user.setResumeText(resumeText.trim());
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
}