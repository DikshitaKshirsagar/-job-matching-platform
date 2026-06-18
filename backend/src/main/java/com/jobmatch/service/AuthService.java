package com.jobmatch.service;

import com.jobmatch.api.dto.request.ForgotPasswordRequest;
import com.jobmatch.api.dto.request.LoginRequest;
import com.jobmatch.api.dto.request.RegisterRequest;
import com.jobmatch.api.dto.request.ResetPasswordRequest;
import com.jobmatch.api.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse saveResume(String email, String resumeText);
    AuthResponse refresh(String refreshToken);
    void verifyEmail(String token);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
