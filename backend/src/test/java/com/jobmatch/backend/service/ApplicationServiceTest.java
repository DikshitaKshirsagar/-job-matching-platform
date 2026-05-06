package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.ApplyRequest;
import com.jobmatch.backend.entity.Application;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.ApplicationStatus;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.ApplicationRepository;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiMatchingService aiMatchingService;

    @InjectMocks
    private ApplicationService applicationService;

    @AfterEach
    void tearDown() {
      SecurityContextHolder.clearContext();
    }

    @Test
    void testApplySuccess() {
        User user = buildSeeker();
        Job job = buildJob();
        ApplyRequest request = new ApplyRequest();
        request.setJobId(5L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, user.getAuthorities())
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByUserAndJob(user, job)).thenReturn(false);
        when(aiMatchingService.getMatchScore(user.getResumeText(), job.getDescription())).thenReturn(84.5);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application app = invocation.getArgument(0);
            app.setId(100L);
            return app;
        });

        var response = applicationService.applyToJob(request);

        assertEquals(100L, response.id());
        assertEquals(84.5, response.matchScore());
        assertEquals("Seeker User", response.applicantName());
    }

    @Test
    void testDuplicateApply() {
        User user = buildSeeker();
        Job job = buildJob();
        ApplyRequest request = new ApplyRequest();
        request.setJobId(5L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, user.getAuthorities())
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByUserAndJob(user, job)).thenReturn(true);

        assertThrows(AppException.class, () -> applicationService.applyToJob(request));
    }

    @Test
    void testGetApplicantsRankedByScore() {
        User recruiter = new User();
        recruiter.setId(99L);
        recruiter.setName("Recruiter");
        recruiter.setEmail("recruiter@example.com");
        recruiter.setRole(Role.RECRUITER);

        Job job = buildJob();
        job.setRecruiterId(99L);

        Application top = new Application();
        top.setId(1L);
        top.setJob(job);
        top.setUser(buildSeeker());
        top.setMatchScore(92.0);
        top.setStatus(ApplicationStatus.APPLIED);

        Application lower = new Application();
        lower.setId(2L);
        lower.setJob(job);
        lower.setUser(buildAnotherSeeker());
        lower.setMatchScore(70.0);
        lower.setStatus(ApplicationStatus.APPLIED);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(recruiter.getEmail(), null, recruiter.getAuthorities())
        );

        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobOrderByMatchScoreDesc(job, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(top, lower)));

        var responses = applicationService.getApplicationsByJob(5L, Pageable.unpaged());

        assertEquals(2, responses.getContent().size());
        assertEquals(92.0, responses.getContent().get(0).matchScore());
        assertEquals("Seeker User", responses.getContent().get(0).applicantName());
    }

    private User buildSeeker() {
        User user = new User();
        user.setId(1L);
        user.setName("Seeker User");
        user.setEmail("seeker@example.com");
        user.setRole(Role.SEEKER);
        user.setResumeText("Java Spring Boot React SQL");
        return user;
    }

    private User buildAnotherSeeker() {
        User user = new User();
        user.setId(2L);
        user.setName("Another User");
        user.setEmail("another@example.com");
        user.setRole(Role.SEEKER);
        user.setResumeText("React HTML CSS");
        return user;
    }

    private Job buildJob() {
        Job job = new Job();
        job.setId(5L);
        job.setTitle("Java Developer");
        job.setCompany("Acme");
        job.setLocation("Remote");
        job.setDescription("Looking for Java Spring Boot SQL experience");
        job.setRecruiterId(99L);
        return job;
    }
}
