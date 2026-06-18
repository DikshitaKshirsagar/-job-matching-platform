package com.jobmatch.service.impl;

import com.jobmatch.api.dto.request.CreateJobRequest;
import com.jobmatch.api.dto.request.UpdateJobRequest;
import com.jobmatch.api.dto.response.JobResponse;
import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.JobStatus;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.JobRepository;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ResourceNotFoundException;
import com.jobmatch.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    private JobServiceImpl jobService;

    private User recruiter;
    private Job activeJob;

    @BeforeEach
    void setUp() {
        jobService = new JobServiceImpl(jobRepository, userRepository);

        recruiter = new User();
        recruiter.setId(1L);
        recruiter.setName("Recruiter");
        recruiter.setEmail("recruiter@company.com");
        recruiter.setRole(UserRole.ROLE_RECRUITER);

        activeJob = new Job();
        activeJob.setId(1L);
        activeJob.setTitle("Software Engineer");
        activeJob.setDescription("Java developer needed");
        activeJob.setCompany("Tech Corp");
        activeJob.setLocation("New York");
        activeJob.setRequiredSkills(List.of("Java", "Spring"));
        activeJob.setRecruiter(recruiter);
        activeJob.setStatus(JobStatus.ACTIVE);
        activeJob.setDeleted(false);
    }

    @Test
    void createJob_whenValidRequest_createsJob() {
        CreateJobRequest request = new CreateJobRequest();
        request.setTitle("Software Engineer");
        request.setDescription("Java developer needed");
        request.setCompany("Tech Corp");
        request.setLocation("New York");
        request.setRequiredSkills(List.of("Java", "Spring"));
        request.setJobType("FULL_TIME");
        request.setSalaryMin(new BigDecimal("80000"));
        request.setSalaryMax(new BigDecimal("120000"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(any(Job.class))).thenReturn(activeJob);

        JobResponse response = jobService.createJob(request, 1L);

        assertNotNull(response);
        assertEquals("Software Engineer", response.getTitle());
        assertEquals("Tech Corp", response.getCompany());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void createJob_whenNullRequest_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> jobService.createJob(null, 1L));
    }

    @Test
    void createJob_whenRecruiterNotFound_throwsResourceNotFound() {
        CreateJobRequest request = new CreateJobRequest();
        request.setTitle("Title");
        request.setDescription("Desc");
        request.setCompany("Company");
        request.setLocation("Location");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.createJob(request, 999L));
    }

    @Test
    void createJob_whenInvalidJobType_throwsBadRequest() {
        CreateJobRequest request = new CreateJobRequest();
        request.setTitle("Title");
        request.setDescription("Desc");
        request.setCompany("Company");
        request.setLocation("Location");
        request.setJobType("INVALID_TYPE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(recruiter));

        assertThrows(BadRequestException.class, () -> jobService.createJob(request, 1L));
    }

    @Test
    void getJobById_whenJobExists_returnsJob() {
        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeJob));

        JobResponse response = jobService.getJobById(1L);

        assertNotNull(response);
        assertEquals("Software Engineer", response.getTitle());
    }

    @Test
    void getJobById_whenJobNotFound_throwsResourceNotFound() {
        when(jobRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.getJobById(999L));
    }

    @Test
    void searchJobs_whenKeywordProvided_returnsFilteredJobs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Job> jobPage = new PageImpl<>(List.of(activeJob));

        when(jobRepository.searchJobs("Java", null, pageable)).thenReturn(jobPage);

        Page<JobResponse> result = jobService.searchJobs("Java", null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Software Engineer", result.getContent().get(0).getTitle());
    }

    @Test
    void getAllActiveJobs_returnsOnlyActiveJobs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Job> jobPage = new PageImpl<>(List.of(activeJob));

        when(jobRepository.findByStatusAndDeletedFalse(JobStatus.ACTIVE, pageable)).thenReturn(jobPage);

        Page<JobResponse> result = jobService.getAllActiveJobs(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateJob_whenValidRequest_updatesJob() {
        UpdateJobRequest request = new UpdateJobRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated description");

        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeJob));
        when(jobRepository.save(any(Job.class))).thenReturn(activeJob);

        JobResponse response = jobService.updateJob(1L, request, 1L);

        assertNotNull(response);
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void updateJob_whenNotOwner_throwsUnauthorized() {
        UpdateJobRequest request = new UpdateJobRequest();
        request.setTitle("Updated Title");

        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeJob));

        assertThrows(UnauthorizedException.class, () -> jobService.updateJob(1L, request, 999L));
    }

    @Test
    void deleteJob_whenOwner_deletesJob() {
        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeJob));

        jobService.deleteJob(1L, 1L);

        assertTrue(activeJob.isDeleted());
        verify(jobRepository).save(activeJob);
    }

    @Test
    void deleteJob_whenNotOwner_throwsUnauthorized() {
        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(activeJob));

        assertThrows(UnauthorizedException.class, () -> jobService.deleteJob(1L, 999L));
    }

    @Test
    void deleteJob_whenJobNotFound_throwsResourceNotFound() {
        when(jobRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.deleteJob(999L, 1L));
    }

    @Test
    void getJobsByRecruiter_returnsOwnedJobs() {
        when(jobRepository.findByRecruiterIdAndDeletedFalse(1L)).thenReturn(List.of(activeJob));

        List<JobResponse> jobs = jobService.getJobsByRecruiter(1L);

        assertEquals(1, jobs.size());
        assertEquals("Software Engineer", jobs.get(0).getTitle());
    }

    @Test
    void getJobRecommendations_returnsActiveJobs() {
        Pageable unpaged = Pageable.unpaged();
        Page<Job> jobPage = new PageImpl<>(List.of(activeJob));
        when(jobRepository.findByStatusAndDeletedFalse(JobStatus.ACTIVE, unpaged)).thenReturn(jobPage);

        List<JobResponse> recommendations = jobService.getJobRecommendations(1L);

        assertFalse(recommendations.isEmpty());
    }

    @Test
    void updateJob_whenJobNotFound_throwsResourceNotFound() {
        UpdateJobRequest request = new UpdateJobRequest();
        request.setTitle("Title");

        when(jobRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.updateJob(999L, request, 1L));
    }
}