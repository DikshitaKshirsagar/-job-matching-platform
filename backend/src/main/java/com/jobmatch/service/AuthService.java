package com.jobmatch.service;

import com.jobmatch.api.dto.request.LoginRequest;
import com.jobmatch.api.dto.request.RegisterRequest;
import com.jobmatch.api.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse saveResume(String email, String resumeText);
}
