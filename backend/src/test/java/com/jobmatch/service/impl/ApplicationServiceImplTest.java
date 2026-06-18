package com.jobmatch.service.impl;

import com.jobmatch.api.dto.request.ApplyJobRequest;
import com.jobmatch.api.dto.response.ApplicationResponse;
import com.jobmatch.domain.entity.Application;
import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.ApplicationStatus;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.ApplicationRepository;
import com.jobmatch.domain.repository.JobRepository;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ResourceNotFoundException;
import com.jobmatch.exception.UnauthorizedException;
import com.jobmatch.infrastructure.external.AiServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiServiceClient aiServiceClient;

    private ApplicationServiceImpl applicationService;

    private User applicant;
    private User recruiter;
    private Job job;
    private Application application;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationServiceImpl(
                applicationRepository, jobRepository, userRepository, aiServiceClient);

        applicant = new User();
        applicant.setId(1L);
        applicant.setName("John Applicant");
        applicant.setEmail("john@example.com");
        applicant.setRole(UserRole.ROLE_JOB_SEEKER);
        applicant.setResumeText("Experienced Java developer");

        recruiter = new User();
        recruiter.setId(2L);
        recruiter.setName("Recruiter Corp");
        recruiter.setEmail("recruiter@company.com");
        recruiter.setRole(UserRole.ROLE_RECRUITER);

        job = new Job();
        job.setId(100L);
        job.setTitle("Software Engineer");
        job.setCompany("Tech Corp");
        job.setLocation("New York");
        job.setDescription("Java developer needed");
        job.setRecruiter(recruiter);
        job.setDeleted(false);

        application = new Application();
        application.setId(1L);
        application.setApplicant(applicant);
        application.setJob(job);
        application.setStatus(ApplicationStatus.PENDING);
        application.setMatchScore(85.0);
        application.setCoverLetter("I am interested");
        application.setDeleted(false);
    }

    @Test
    void applyToJob_whenValidRequest_createsApplicationWithMatchScore() {
        Long applicantId = 1L;
        Long jobId = 100L;

        ApplyJobRequest request = new ApplyJobRequest();
        request.setJobId(jobId);
        request.setCoverLetter("I am interested in this role");

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByApplicantIdAndJobId(applicantId, jobId)).thenReturn(false);
        when(aiServiceClient.calculateMatchScore(anyString(), anyString())).thenReturn(87.5);

        Application savedApplication = new Application();
        savedApplication.setId(1L);
        savedApplication.setApplicant(applicant);
        savedApplication.setJob(job);
        savedApplication.setStatus(ApplicationStatus.PENDING);
        savedApplication.setMatchScore(87.5);
        savedApplication.setCoverLetter("I am interested in this role");
        when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

        ApplicationResponse response = applicationService.applyToJob(request, applicantId);

        assertNotNull(response);
        verify(aiServiceClient).calculateMatchScore(
                "Experienced Java developer",
                "Java developer needed");
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void applyToJob_whenNullRequest_throwsException() {
        assertThrows(NullPointerException.class, () -> applicationService.applyToJob(null, 1L));
    }

    @Test
    void applyToJob_whenJobIdIsNull_throwsBadRequest() {
        ApplyJobRequest request = new ApplyJobRequest();
        request.setJobId(null);

        assertThrows(BadRequestException.class, () -> applicationService.applyToJob(request, 1L));
    }

    @Test
    void applyToJob_whenDuplicateApplication_throwsBadRequest() {
        Long applicantId = 1L;
        Long jobId = 100L;

        ApplyJobRequest request = new ApplyJobRequest();
        request.setJobId(jobId);

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByApplicantIdAndJobId(applicantId, jobId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> applicationService.applyToJob(request, applicantId));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyToJob_whenUserNotFound_throwsResourceNotFound() {
        Long applicantId = 999L;
        ApplyJobRequest request = new ApplyJobRequest();
        request.setJobId(100L);

        when(userRepository.findById(applicantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> applicationService.applyToJob(request, applicantId));
    }

    @Test
    void applyToJob_whenJobNotFound_throwsResourceNotFound() {
        Long applicantId = 1L;
        Long jobId = 999L;
        ApplyJobRequest request = new ApplyJobRequest();
        request.setJobId(jobId);

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(jobRepository.findByIdAndDeletedFalse(jobId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> applicationService.applyToJob(request, applicantId));
    }

    @Test
    void getMyApplications_whenUserHasApplications_returnsPaginatedList() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> applicationPage = new PageImpl<>(List.of(application));

        when(applicationRepository.findByApplicantIdAndDeletedFalse(userId, pageable)).thenReturn(applicationPage);

        Page<ApplicationResponse> result = applicationService.getMyApplications(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("Software Engineer", result.getContent().get(0).getJobTitle());
        verify(applicationRepository).findByApplicantIdAndDeletedFalse(userId, pageable);
    }

    @Test
    void getMyApplications_whenNoApplications_returnsEmptyPage() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> emptyPage = new PageImpl<>(List.of());

        when(applicationRepository.findByApplicantIdAndDeletedFalse(userId, pageable)).thenReturn(emptyPage);

        Page<ApplicationResponse> result = applicationService.getMyApplications(userId, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getMyApplicationsList_whenUserHasApplications_returnsList() {
        Long userId = 1L;

        when(applicationRepository.findByApplicantOrderByCreatedAtDesc(userId)).thenReturn(List.of(application));

        List<ApplicationResponse> result = applicationService.getMyApplicationsList(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Software Engineer", result.get(0).getJobTitle());
        verify(applicationRepository).findByApplicantOrderByCreatedAtDesc(userId);
    }

    @Test
    void getMyApplicationsList_whenNoApplications_returnsEmptyList() {
        Long userId = 1L;

        when(applicationRepository.findByApplicantOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        List<ApplicationResponse> result = applicationService.getMyApplicationsList(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getApplicationsByJob_whenValidRecruiter_returnsPaginatedApplications() {
        Long jobId = 100L;
        Long recruiterId = 2L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> applicationPage = new PageImpl<>(List.of(application));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.findById(recruiterId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findByJobOrderByMatchScoreDesc(jobId, pageable)).thenReturn(applicationPage);

        Page<ApplicationResponse> result = applicationService.getApplicationsByJob(jobId, recruiterId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        verify(applicationRepository).findByJobOrderByMatchScoreDesc(jobId, pageable);
    }

    @Test
    void getApplicationsByJob_whenJobNotFound_throwsResourceNotFound() {
        Long jobId = 999L;
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.getApplicationsByJob(jobId, 2L, PageRequest.of(0, 10)));
    }

    @Test
    void getApplicationsByJob_whenRecruiterNotFound_throwsResourceNotFound() {
        Long jobId = 100L;
        Long recruiterId = 999L;

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.findById(recruiterId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.getApplicationsByJob(jobId, recruiterId, PageRequest.of(0, 10)));
    }

    @Test
    void getApplicationsByJob_whenNotARecruiter_throwsUnauthorized() {
        Long jobId = 100L;
        Long seekerId = 1L;

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(userRepository.findById(seekerId)).thenReturn(Optional.of(applicant));

        assertThrows(UnauthorizedException.class,
                () -> applicationService.getApplicationsByJob(jobId, seekerId, PageRequest.of(0, 10)));
    }

    @Test
    void getApplicationsByJob_whenNotJobOwner_throwsUnauthorized() {
        Long jobId = 100L;
        User otherRecruiter = new User();
        otherRecruiter.setId(3L);
        otherRecruiter.setRole(UserRole.ROLE_RECRUITER);

        Job otherJob = new Job();
        otherJob.setId(200L);
        User ownerRecruiter = new User();
        ownerRecruiter.setId(5L);
        otherJob.setRecruiter(ownerRecruiter);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(otherJob));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherRecruiter));

        assertThrows(UnauthorizedException.class,
                () -> applicationService.getApplicationsByJob(jobId, 3L, PageRequest.of(0, 10)));
    }

    @Test
    void updateApplicationStatus_whenValidRequest_updatesStatus() {
        Long applicationId = 1L;
        Long recruiterId = 2L;

        when(userRepository.findById(recruiterId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        ApplicationResponse response = applicationService.updateApplicationStatus(applicationId, "SHORTLISTED", recruiterId);

        assertNotNull(response);
        assertEquals(ApplicationStatus.SHORTLISTED, response.getStatus());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void updateApplicationStatus_whenRecruiterNotFound_throwsResourceNotFound() {
        Long applicationId = 1L;
        Long recruiterId = 999L;

        when(userRepository.findById(recruiterId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.updateApplicationStatus(applicationId, "SHORTLISTED", recruiterId));
    }

    @Test
    void updateApplicationStatus_whenNotARecruiter_throwsUnauthorized() {
        Long applicationId = 1L;
        Long seekerId = 1L;

        when(userRepository.findById(seekerId)).thenReturn(Optional.of(applicant));

        assertThrows(UnauthorizedException.class,
                () -> applicationService.updateApplicationStatus(applicationId, "SHORTLISTED", seekerId));
    }

    @Test
    void updateApplicationStatus_whenApplicationNotFound_throwsResourceNotFound() {
        Long applicationId = 999L;
        Long recruiterId = 2L;

        when(userRepository.findById(recruiterId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.updateApplicationStatus(applicationId, "SHORTLISTED", recruiterId));
    }

    @Test
    void updateApplicationStatus_whenNotJobOwner_throwsUnauthorized() {
        Long applicationId = 1L;
        User otherRecruiter = new User();
        otherRecruiter.setId(3L);
        otherRecruiter.setRole(UserRole.ROLE_RECRUITER);

        User jobOwnerRecruiter = new User();
        jobOwnerRecruiter.setId(5L);
        jobOwnerRecruiter.setRole(UserRole.ROLE_RECRUITER);

        Job otherJob = new Job();
        otherJob.setRecruiter(jobOwnerRecruiter);
        application.setJob(otherJob);

        when(userRepository.findById(3L)).thenReturn(Optional.of(otherRecruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(UnauthorizedException.class,
                () -> applicationService.updateApplicationStatus(applicationId, "SHORTLISTED", 3L));
    }

    @Test
    void updateApplicationStatus_whenInvalidStatus_throwsBadRequest() {
        Long applicationId = 1L;
        Long recruiterId = 2L;

        when(userRepository.findById(recruiterId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class,
                () -> applicationService.updateApplicationStatus(applicationId, "INVALID_STATUS", recruiterId));
    }

    @Test
    void updateApplicationStatus_whenRejected_updatesStatus() {
        Long applicationId = 1L;
        Long recruiterId = 2L;

        when(userRepository.findById(recruiterId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        ApplicationResponse response = applicationService.updateApplicationStatus(applicationId, "REJECTED", recruiterId);

        assertNotNull(response);
        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
    }

    @Test
    void updateApplicationStatus_whenHired_updatesStatus() {
        Long applicationId = 1L;
        Long recruiterId = 2L;

        when(userRepository.findById(recruiterId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        ApplicationResponse response = applicationService.updateApplicationStatus(applicationId, "HIRED", recruiterId);

        assertNotNull(response);
        assertEquals(ApplicationStatus.HIRED, response.getStatus());
    }
}