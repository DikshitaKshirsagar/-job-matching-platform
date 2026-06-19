package com.jobmatch.service.impl;

import com.jobmatch.api.dto.request.LoginRequest;
import com.jobmatch.api.dto.request.RegisterRequest;
import com.jobmatch.api.dto.response.AuthResponse;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ConflictException;
import com.jobmatch.exception.UnauthorizedException;
import com.jobmatch.infrastructure.security.JwtTokenProvider;
import com.jobmatch.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtTokenProvider, emailService);
    }

    @Test
    void register_whenValidRequest_createsUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole("JOB_SEEKER");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setRole(UserRole.ROLE_JOB_SEEKER);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateTokenFromEmail("john@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.generateRefreshTokenFromEmail("john@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(UserRole.ROLE_JOB_SEEKER, response.getRole());
        assertEquals(1L, response.getUserId());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void register_whenNullRequest_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.register(null));
    }

    @Test
    void register_whenPasswordsDoNotMatch_throwsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("different");
        request.setRole("JOB_SEEKER");

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void register_whenEmailAlreadyExists_throwsConflictException() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setRole("JOB_SEEKER");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
    }

    @Test
    void register_whenTokenGenerationFails_returnsResponseWithNullToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole("JOB_SEEKER");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setRole(UserRole.ROLE_JOB_SEEKER);

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateTokenFromEmail("john@example.com")).thenThrow(new RuntimeException("JWT error"));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNull(response.getToken());
    }

    @Test
    void login_whenValidCredentials_returnsToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encoded-password");
        user.setRole(UserRole.ROLE_JOB_SEEKER);
        user.setEmailVerified(true);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateTokenFromEmail("john@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.generateRefreshTokenFromEmail("john@example.com")).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(user);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_whenNullRequest_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.login(null));
    }

    @Test
    void login_whenUserNotFound_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_whenInvalidPassword_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrong-password");

        User user = new User();
        user.setEmail("john@example.com");
        user.setPassword("encoded-password");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void saveResume_whenValidRequest_savesResumeAndReturnsResponse() {
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setRole(UserRole.ROLE_JOB_SEEKER);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.saveResume("john@example.com", "Experienced Java developer");

        assertNotNull(response);
        assertEquals("Resume uploaded successfully", response.getMessage());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void saveResume_whenNullEmail_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.saveResume(null, "resume text"));
    }

    @Test
    void saveResume_whenBlankResume_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.saveResume("john@example.com", "   "));
    }

    @Test
    void register_whenRecruiterRole_createsRecruiterUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Recruiter Bob");
        request.setEmail("bob@company.com");
        request.setPassword("password123");
        request.setRole("RECRUITER");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setName("Recruiter Bob");
        savedUser.setEmail("bob@company.com");
        savedUser.setRole(UserRole.ROLE_RECRUITER);

        when(userRepository.existsByEmail("bob@company.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateTokenFromEmail("bob@company.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.generateRefreshTokenFromEmail("bob@company.com")).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(UserRole.ROLE_RECRUITER, response.getRole());
    }

    @Test
    void register_whenInvalidRole_throwsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole("INVALID_ROLE_XYZ");

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void register_whenNullFullName_throwsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(null);
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole("JOB_SEEKER");

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void refresh_whenValidToken_returnsNewTokens() {
        String refreshToken = "valid-refresh-token";
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setRole(UserRole.ROLE_JOB_SEEKER);
        user.setRefreshToken(refreshToken);

        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromRefreshToken(refreshToken)).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateTokenFromEmail("john@example.com")).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshTokenFromEmail("john@example.com")).thenReturn("new-refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(user);

        AuthResponse response = authService.refresh(refreshToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("John Doe", response.getName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void refresh_whenNullToken_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.refresh(null));
    }

    @Test
    void refresh_whenInvalidToken_throwsUnauthorized() {
        when(jwtTokenProvider.validateRefreshToken("invalid-token")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refresh("invalid-token"));
    }

    @Test
    void refresh_whenTokenMismatch_throwsUnauthorized() {
        String refreshToken = "valid-refresh-token";
        User user = new User();
        user.setEmail("john@example.com");
        user.setRefreshToken("different-refresh-token");

        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromRefreshToken(refreshToken)).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.refresh(refreshToken));
    }
}