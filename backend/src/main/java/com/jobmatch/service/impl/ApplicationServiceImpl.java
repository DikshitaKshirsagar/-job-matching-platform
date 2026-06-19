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
import com.jobmatch.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;

    @Override
    @Transactional
    public ApplicationResponse applyToJob(ApplyJobRequest request, Long applicantId) {
        log.info("User id: {} applying to job id: {}", applicantId, request.getJobId());

        if (request == null || request.getJobId() == null) {
            throw new BadRequestException("Job id is required");
        }

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", applicantId));

        Job job = jobRepository.findByIdAndDeletedFalse(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", request.getJobId()));

        if (applicationRepository.existsByApplicantIdAndJobId(applicantId, request.getJobId())) {
            log.warn("User id: {} already applied to job id: {}", applicantId, request.getJobId());
            throw new BadRequestException("You have already applied to this job");
        }

        // Compute match score via AI service (gracefully falls back to 0.0 if unavailable)
        double matchScore = aiServiceClient.calculateMatchScore(
                applicant.getResumeText(),
                job.getDescription()
        );
        log.info("Computed match score for job id {}: {}", job.getId(), matchScore);

        Application application = new Application();
        application.setApplicant(applicant);
        application.setJob(job);
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(ApplicationStatus.PENDING);
        application.setMatchScore(matchScore);

        Application saved = applicationRepository.save(application);
        log.info("Successfully created application with id: {}, matchScore: {}", saved.getId(), matchScore);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(Long userId, Pageable pageable) {
        log.debug("Fetching applications for user id: {}", userId);
        return applicationRepository.findByApplicantIdAndDeletedFalse(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplicationsList(Long userId) {
        log.debug("Fetching all applications list for user id: {}", userId);
        return applicationRepository.findByApplicantIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByJob(Long jobId, Long recruiterId, Pageable pageable) {
        log.debug("Fetching applications for job id: {}", jobId);
        
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", recruiterId));

        if (!recruiter.getRole().equals(com.jobmatch.domain.enums.UserRole.ROLE_RECRUITER)) {
            throw new com.jobmatch.exception.UnauthorizedException("Only recruiters can view job applicants");
        }

        if (!job.getRecruiter().getId().equals(recruiterId)) {
            log.warn("Unauthorized access attempt for job id: {} by user id: {}", jobId, recruiterId);
            throw new com.jobmatch.exception.UnauthorizedException("You can only view applications for your own jobs");
        }

        return applicationRepository.findByJobIdOrderByMatchScoreDesc(jobId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, String status, Long recruiterId) {
        log.info("Updating application id: {} status to: {} by recruiter id: {}", applicationId, status, recruiterId);

        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", recruiterId));

        if (!recruiter.getRole().equals(UserRole.ROLE_RECRUITER)) {
            throw new UnauthorizedException("Only recruiters can update application status");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (!application.getJob().getRecruiter().getId().equals(recruiterId)) {
            log.warn("Unauthorized status update attempt for application id: {} by recruiter id: {}", applicationId, recruiterId);
            throw new UnauthorizedException("You can only update applications for your jobs");
        }

        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid application status: " + status);
        }

        application.setStatus(newStatus);
        Application updated = applicationRepository.save(application);
        log.info("Successfully updated application with id: {}", updated.getId());
        return toResponse(updated);
    }

    private ApplicationResponse toResponse(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .company(application.getJob().getCompany())
                .location(application.getJob().getLocation())
                .matchScore(application.getMatchScore())
                .status(application.getStatus())
                .appliedAt(application.getCreatedAt())
                .applicantName(application.getApplicant().getName())
                .applicantEmail(application.getApplicant().getEmail())
                .build();
    }
}
