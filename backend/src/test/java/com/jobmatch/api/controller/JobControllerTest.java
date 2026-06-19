package com.jobmatch.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.api.dto.request.CreateJobRequest;
import com.jobmatch.api.dto.request.UpdateJobRequest;
import com.jobmatch.api.dto.response.JobResponse;
import com.jobmatch.domain.enums.JobStatus;
import com.jobmatch.domain.enums.JobType;
import com.jobmatch.exception.GlobalExceptionHandler;
import com.jobmatch.service.JobService;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JobService jobService;

    @Mock
    private UserIdResolver userIdResolver;

    @InjectMocks
    private JobController jobController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(jobController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private JobResponse createSampleJobResponse() {
        return JobResponse.builder()
                .id(1L)
                .title("Software Engineer")
                .description("Java developer needed")
                .company("Tech Corp")
                .location("New York")
                .requiredSkills(List.of("Java", "Spring"))
                .jobType(JobType.FULL_TIME)
                .salaryMin(new BigDecimal("80000"))
                .salaryMax(new BigDecimal("120000"))
                .status(JobStatus.ACTIVE)
                .recruiterId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createJob_whenValidRequest_returns201() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(jobService.createJob(any(CreateJobRequest.class), eq(1L))).thenReturn(createSampleJobResponse());

        CreateJobRequest request = new CreateJobRequest();
        request.setTitle("Software Engineer");
        request.setDescription("We are looking for an experienced Java developer to join our growing team and build scalable applications.");
        request.setCompany("Tech Corp");
        request.setLocation("New York");
        request.setRequiredSkills(List.of("Java", "Spring"));
        request.setJobType("FULL_TIME");
        request.setSalaryMin(new BigDecimal("80000"));
        request.setSalaryMax(new BigDecimal("120000"));

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Job created successfully"))
                .andExpect(jsonPath("$.data.title").value("Software Engineer"))
                .andExpect(jsonPath("$.data.company").value("Tech Corp"));
    }

    @Test
    void getAllJobs_whenNoFilters_returnsAllJobs() throws Exception {
        Page<JobResponse> jobPage = new PageImpl<>(List.of(createSampleJobResponse()));
        when(jobService.searchJobs(any(), any(), any())).thenReturn(jobPage);

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void getJobById_whenJobExists_returnsJob() throws Exception {
        when(jobService.getJobById(1L)).thenReturn(createSampleJobResponse());

        mockMvc.perform(get("/api/v1/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.title").value("Software Engineer"));
    }

    @Test
    void updateJob_whenValidRequest_returnsOk() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(jobService.updateJob(anyLong(), any(UpdateJobRequest.class), eq(1L))).thenReturn(createSampleJobResponse());

        UpdateJobRequest request = new UpdateJobRequest();
        request.setTitle("Updated Title");

        mockMvc.perform(put("/api/v1/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Job updated successfully"));
    }

    @Test
    void deleteJob_whenValidRequest_returnsOk() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Job deleted successfully"));
    }

    @Test
    void getMyJobs_whenJobsExist_returnsList() throws Exception {
        when(userIdResolver.getCurrentUserId()).thenReturn(1L);
        when(jobService.getJobsByRecruiter(1L)).thenReturn(List.of(createSampleJobResponse()));

        mockMvc.perform(get("/api/v1/jobs/recruiter/my-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}