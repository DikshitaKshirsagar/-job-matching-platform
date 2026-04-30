package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.LoginRequest;
import com.jobmatch.backend.dto.RegisterRequest;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.UserRepository;
import com.jobmatch.backend.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Recruiter User");
        registerRequest.setEmail("recruiter@example.com");
        registerRequest.setPassword("Test1234");
        registerRequest.setRole("RECRUITER");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("recruiter@example.com");
        loginRequest.setPassword("Test1234");

        user = new User();
        user.setId(7L);
        user.setName("Recruiter User");
        user.setEmail("recruiter@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.RECRUITER);
        user.setEmailVerified(true);
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("recruiter@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Test1234")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        var response = authService.register(registerRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals(Role.RECRUITER, response.getRole());
        assertEquals("recruiter@example.com", response.getEmail());
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test1234", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        var response = authService.login(loginRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals(Role.RECRUITER, response.getRole());
    }

    @Test
    void testLoginWrongPassword() {
        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test1234", "encoded-password")).thenReturn(false);

        assertThrows(AppException.class, () -> authService.login(loginRequest));
    }
}
