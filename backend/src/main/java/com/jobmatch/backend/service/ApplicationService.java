package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.ApplicationResponse;
import com.jobmatch.backend.dto.ApplyRequest;
import com.jobmatch.backend.entity.Application;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.ApplicationRepository;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;
import com.jobmatch.backend.entity.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    // ✅ APPLY TO JOB
    @Transactional
    public ApplicationResponse applyToJob(ApplyRequest request) {

        if (request == null || request.getJobId() == null) {
            throw new AppException("Job id is required", HttpStatus.BAD_REQUEST);
        }

        Long jobId = request.getJobId();

        User user = getCurrentUser();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException("Job not found", HttpStatus.NOT_FOUND));

        // ❗ Prevent duplicate applications
        if (applicationRepository.existsByUserAndJob(user, job)) {
            throw new AppException("Already applied to this job", HttpStatus.BAD_REQUEST);
        }

        double matchScore = 0.0;

        // ✅ CALL AI SERVICE (IMPROVED)
        try {
            if (user.getResumeText() != null && !user.getResumeText().isBlank()) {

                String url = aiServiceUrl + "/match";

                Map<String, String> body = new HashMap<>();
                body.put("resumeText", user.getResumeText());
                body.put("jobDescription", job.getDescription());

                ResponseEntity<Map> response = restTemplate.postForEntity(
                        url,
                        body,
                        Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null) {

                    Object scoreObj = response.getBody().get("matchScore");
                    if (scoreObj != null) {
                        String scoreStr = scoreObj.toString();
                        try {
                            double rawScore = Double.parseDouble(scoreStr);
                            // Clamp to valid range 0-100
                            matchScore = Math.max(0.0, Math.min(100.0, rawScore));
                            log.debug("AI match score: {} for job {}", matchScore, job.getId());
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid AI score format: '{}', defaulting to 0.0 for job {}", scoreStr, job.getId());
                        }
                    } else {
                        log.warn("No matchScore in AI response for job {}", job.getId());
                    }
                } else {
                    log.warn("AI service bad status {} for job {}", response.getStatusCode(), job.getId());
                }
            } else {
                log.debug("No resume text for AI matching, score=0.0");
            }
        } catch (Exception e) {
            log.error("AI service unreachable: {} - using matchScore=0.0 for job {}",
                      e.getMessage(), job.getId());
        }

        // ✅ SAVE APPLICATION
        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setMatchScore(matchScore);
        application.setStatus("APPLIED");

        Application saved = applicationRepository.save(application);

        return new ApplicationResponse(
                saved.getId(),
                saved.getJob().getId(),
                saved.getJob().getTitle(),
                saved.getJob().getCompany(),
                saved.getJob().getLocation(),
                saved.getStatus(),
                saved.getAppliedAt()
        );
    }

    // ✅ GET MY APPLICATIONS (SEEKER)
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications() {

        User user = getCurrentUser();

        return applicationRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ✅ GET APPLICATIONS BY JOB (RECRUITER)
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByJob(Long jobId) {

        User currentUser = getCurrentUser();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException("Job not found", HttpStatus.NOT_FOUND));

        // ❗ Only recruiter allowed
        if (currentUser.getRole() != Role.RECRUITER) {
            throw new AppException("Only recruiters can view job applications", HttpStatus.FORBIDDEN);
        }

        // ❗ Only own job
        if (!job.getRecruiterId().equals(currentUser.getId())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        // ✅ SORTED BY MATCH SCORE
        return applicationRepository.findByJobOrderByMatchScoreDesc(job)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ✅ GET CURRENT USER
    private User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    // ✅ CONVERT ENTITY → DTO
    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getJob().getCompany(),
                application.getJob().getLocation(),
                application.getStatus(),
                application.getAppliedAt()
        );
    }
}
