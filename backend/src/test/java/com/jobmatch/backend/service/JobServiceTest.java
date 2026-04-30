package com.jobmatch.backend.service;

import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiMatchingService aiMatchingService;

    @InjectMocks
    private JobService jobService;

    @Test
    void testCreateJob() {
        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setCompany("Acme");
        job.setDescription("Java Spring Boot role");
        job.setLocation("Remote");
        job.setSalary("12 LPA");

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job created = jobService.createJob(job, "RECRUITER", 11L);

        assertEquals(11L, created.getRecruiterId());
        assertEquals("Backend Engineer", created.getTitle());
    }

    @Test
    void testGetAllJobs() {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Frontend Engineer");
        job.setCompany("Acme");
        job.setDescription("React job");

        when(jobRepository.findAll()).thenReturn(List.of(job));

        var jobs = jobService.getAllJobs();

        assertEquals(1, jobs.size());
        assertEquals("Frontend Engineer", jobs.get(0).title());
    }

    @Test
    void testCreateJobForbiddenForNonRecruiter() {
        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setCompany("Acme");
        job.setDescription("Java Spring Boot role");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> jobService.createJob(job, "SEEKER", 11L)
        );

        assertEquals(FORBIDDEN, exception.getStatusCode());
    }
}
