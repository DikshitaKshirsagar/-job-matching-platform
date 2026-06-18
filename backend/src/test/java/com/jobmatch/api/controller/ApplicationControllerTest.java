package com.jobmatch.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.api.dto.request.ApplyJobRequest;
import com.jobmatch.api.dto.response.ApplicationResponse;
import com.jobmatch.domain.enums.ApplicationStatus;
import com.jobmatch.exception.GlobalExceptionHandler;
import com.jobmatch.service.ApplicationService;
import com.jobmatch.util.UserIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private UserIdResolver userIdResolver;

    @InjectMocks
    private ApplicationController applicationController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(applicationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ApplicationResponse createSampleResponse() {
        return ApplicationResponse.builder()
                .id(1L)
                .jobId(100L)
                .jobTitle("Software Engineer")
                .company("Tech Corp")
                .location("New York")
                .matchScore(85.5)
                .status(ApplicationStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .applicantName("John Doe")
                .applicantEmail("john@example.com")
                .build();
    }

    @Test
    void applyToJob_whenValidRequest_returns201() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(applicationService.applyToJob(any(ApplyJobRequest.class), eq(1L))).thenReturn(createSampleResponse());

        ApplyJobRequest request = new ApplyJobRequest();
        request.setJobId(100L);
        request.setCoverLetter("I am interested");

        mockMvc.perform(post("/api/v1/applications/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Application submitted successfully"))
                .andExpect(jsonPath("$.data.jobTitle").value("Software Engineer"));
    }

    @Test
    void getMyApplications_whenApplicationsExist_returnsList() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(applicationService.getMyApplicationsList(1L)).thenReturn(List.of(createSampleResponse()));

        mockMvc.perform(get("/api/v1/applications/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void getMyApplicationsPaginated_whenApplicationsExist_returnsPage() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        Page<ApplicationResponse> page = new PageImpl<>(List.of(createSampleResponse()));
        when(applicationService.getMyApplications(anyLong(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/applications/my-paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void getJobApplications_whenApplicationsExist_returnsPage() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        Page<ApplicationResponse> page = new PageImpl<>(List.of(createSampleResponse()));
        when(applicationService.getApplicationsByJob(anyLong(), anyLong(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/applications/job/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void updateApplicationStatus_whenValidRequest_returnsOk() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(applicationService.updateApplicationStatus(anyLong(), anyString(), eq(1L)))
                .thenReturn(createSampleResponse());

        mockMvc.perform(patch("/api/v1/applications/1/status")
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Application status updated"));
    }
}