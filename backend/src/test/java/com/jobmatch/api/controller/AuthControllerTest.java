package com.jobmatch.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.api.dto.request.LoginRequest;
import com.jobmatch.api.dto.request.RegisterRequest;
import com.jobmatch.api.dto.response.AuthResponse;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ConflictException;
import com.jobmatch.exception.GlobalExceptionHandler;
import com.jobmatch.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_whenValidRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        request.setRole("JOB_SEEKER");

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token")
                .name("John Doe")
                .email("john@example.com")
                .role(UserRole.ROLE_JOB_SEEKER)
                .userId(1L)
                .message("Registration successful")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    void register_whenConflictException_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("existing@example.com");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        request.setRole("JOB_SEEKER");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Email already exists. Please sign in instead."));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already exists. Please sign in instead."));
    }

    @Test
    void register_whenBadRequestException_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        request.setRole("JOB_SEEKER");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Invalid role: INVALID"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_whenValidCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password123");

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token")
                .name("John Doe")
                .email("john@example.com")
                .role(UserRole.ROLE_JOB_SEEKER)
                .userId(1L)
                .message("Login successful")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test
    void login_whenInvalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("Password123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new com.jobmatch.exception.UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}