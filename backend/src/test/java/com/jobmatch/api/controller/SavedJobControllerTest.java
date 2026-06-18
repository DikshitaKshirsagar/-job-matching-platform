package com.jobmatch.api.controller;

import com.jobmatch.api.dto.response.SavedJobResponse;
import com.jobmatch.exception.GlobalExceptionHandler;
import com.jobmatch.service.SavedJobService;
import com.jobmatch.util.UserIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SavedJobControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SavedJobService savedJobService;

    @Mock
    private UserIdResolver userIdResolver;

    @InjectMocks
    private SavedJobController savedJobController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(savedJobController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SavedJobResponse createSampleResponse() {
        return SavedJobResponse.builder()
                .jobId(100L)
                .title("Software Engineer")
                .company("Tech Corp")
                .location("New York")
                .salary("$80,000 - $120,000")
                .savedAt(LocalDateTime.now())
                .recruiterId(1L)
                .build();
    }

    @Test
    void saveJob_whenValidRequest_returns201() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(savedJobService.saveJob(anyLong(), anyLong())).thenReturn(createSampleResponse());

        mockMvc.perform(post("/api/v1/saved-jobs/100"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Job saved successfully"))
                .andExpect(jsonPath("$.data.jobId").value(100))
                .andExpect(jsonPath("$.data.title").value("Software Engineer"));
    }

    @Test
    void unsaveJob_whenValidRequest_returnsOk() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/saved-jobs/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Job removed from saved"));
    }

    @Test
    void getSavedJobs_whenJobsExist_returnsList() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(savedJobService.getSavedJobs(1L)).thenReturn(List.of(createSampleResponse()));

        mockMvc.perform(get("/api/v1/saved-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Software Engineer"));
    }
}