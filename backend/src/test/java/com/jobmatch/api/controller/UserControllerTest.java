package com.jobmatch.api.controller;

import com.jobmatch.api.dto.response.UserResponse;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.exception.GlobalExceptionHandler;
import com.jobmatch.service.UserService;
import com.jobmatch.util.UserIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private UserIdResolver userIdResolver;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserResponse createSampleResponse() {
        return UserResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(UserRole.ROLE_JOB_SEEKER)
                .hasResume(true)
                .resumeFileName("resume.pdf")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getUserProfile_whenAuthenticated_returnsProfile() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(createSampleResponse());

        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.name").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    void updateUserProfile_whenValidRequest_returnsUpdated() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(userService.updateUserProfile(anyLong(), anyString())).thenReturn(createSampleResponse());

        String json = "{\"name\":\"Updated Name\"}";

        mockMvc.perform(patch("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));
    }

    @Test
    void getDashboard_whenAuthenticated_returnsDashboard() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(userService.getUserProfile(1L)).thenReturn(createSampleResponse());

        mockMvc.perform(get("/api/v1/users/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.user.name").value("John Doe"));
    }
}