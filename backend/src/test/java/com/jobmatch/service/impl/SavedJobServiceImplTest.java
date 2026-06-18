package com.jobmatch.service.impl;

import com.jobmatch.api.dto.response.SavedJobResponse;
import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.SavedJob;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.JobRepository;
import com.jobmatch.domain.repository.SavedJobRepository;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    private SavedJobServiceImpl savedJobService;

    private User user;
    private Job job;
    private SavedJob savedJob;

    @BeforeEach
    void setUp() {
        savedJobService = new SavedJobServiceImpl(userRepository, jobRepository, savedJobRepository);

        user = new User();
        user.setId(1L);
        user.setName("Job Seeker");
        user.setEmail("seeker@example.com");
        user.setRole(UserRole.ROLE_JOB_SEEKER);

        User recruiter = new User();
        recruiter.setId(2L);
        recruiter.setName("Recruiter");
        recruiter.setRole(UserRole.ROLE_RECRUITER);

        job = new Job();
        job.setId(100L);
        job.setTitle("Software Engineer");
        job.setCompany("Tech Corp");
        job.setLocation("New York");
        job.setRecruiter(recruiter);

        savedJob = new SavedJob();
        savedJob.setId(10L);
        savedJob.setUser(user);
        savedJob.setJob(job);
        savedJob.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void saveJob_whenValidRequest_createsSavedJob() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(job));
        when(savedJobRepository.existsByUserAndJob(user, job)).thenReturn(false);
        when(savedJobRepository.save(any(SavedJob.class))).thenReturn(savedJob);

        SavedJobResponse response = savedJobService.saveJob(100L, 1L);

        assertNotNull(response);
        assertEquals(100L, response.getJobId());
        assertEquals("Software Engineer", response.getTitle());
        assertEquals("Tech Corp", response.getCompany());
    }

    @Test
    void saveJob_whenAlreadySaved_throwsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(job));
        when(savedJobRepository.existsByUserAndJob(user, job)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> savedJobService.saveJob(100L, 1L));
    }

    @Test
    void saveJob_whenUserNotFound_throwsResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> savedJobService.saveJob(100L, 999L));
    }

    @Test
    void saveJob_whenJobNotFound_throwsResourceNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> savedJobService.saveJob(999L, 1L));
    }

    @Test
    void unsaveJob_whenValidRequest_deletesSavedJob() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(savedJobRepository.existsByUserAndJob(user, job)).thenReturn(true);

        savedJobService.unsaveJob(100L, 1L);

        verify(savedJobRepository).deleteByUserAndJob(user, job);
    }

    @Test
    void unsaveJob_whenNotSaved_throwsResourceNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(savedJobRepository.existsByUserAndJob(user, job)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> savedJobService.unsaveJob(100L, 1L));
    }

    @Test
    void unsaveJob_whenUserNotFound_throwsResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> savedJobService.unsaveJob(100L, 999L));
    }

    @Test
    void getSavedJobs_whenUserExists_returnsSavedJobs() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(savedJobRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(savedJob));

        List<SavedJobResponse> responses = savedJobService.getSavedJobs(1L);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals("Software Engineer", responses.get(0).getTitle());
    }

    @Test
    void getSavedJobs_whenUserNotFound_throwsResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> savedJobService.getSavedJobs(999L));
    }
}