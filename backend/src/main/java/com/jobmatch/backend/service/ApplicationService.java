package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.ApplicationResponse;
import com.jobmatch.backend.dto.ApplyRequest;
import com.jobmatch.backend.entity.Application;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.ApplicationRepository;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AiMatchingService aiMatchingService;

    @Transactional
    public ApplicationResponse applyToJob(ApplyRequest request) {
        if (request == null || request.getJobId() == null) {
            throw new AppException("Job id is required", HttpStatus.BAD_REQUEST);
        }

        User user = getCurrentUser();

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new AppException("Job not found", HttpStatus.NOT_FOUND));

        if (applicationRepository.existsByUserAndJob(user, job)) {
            throw new AppException("Already applied to this job", HttpStatus.BAD_REQUEST);
        }

        double matchScore = aiMatchingService.getMatchScore(user.getResumeText(), job.getDescription());

        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setMatchScore(matchScore);
        application.setStatus("APPLIED");

        return toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications() {
        User user = getCurrentUser();

        return applicationRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByJob(Long jobId) {
        User currentUser = getCurrentUser();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException("Job not found", HttpStatus.NOT_FOUND));

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new AppException("Only recruiters can view job applications", HttpStatus.FORBIDDEN);
        }

        if (!job.getRecruiterId().equals(currentUser.getId())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        return applicationRepository.findByJobOrderByMatchScoreDesc(job)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getJob().getCompany(),
                application.getJob().getLocation(),
                application.getMatchScore(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getUser().getName(),
                application.getUser().getEmail()
        );
    }
}
