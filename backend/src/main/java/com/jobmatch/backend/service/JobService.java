package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.JobListResponse;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AiMatchingService aiMatchingService;

    public Job createJob(Job job, String role, Long recruiterId) {
        if (!"RECRUITER".equals(role) || recruiterId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can post jobs");
        }

        if (job == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job payload is required");
        }

        if (job.getTitle() == null || job.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job title is required");
        }

        if (job.getCompany() == null || job.getCompany().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company is required");
        }

        if (job.getDescription() == null || job.getDescription().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job description is required");
        }

        job.setTitle(job.getTitle().trim());
        job.setCompany(job.getCompany().trim());
        job.setDescription(job.getDescription().trim());
        job.setLocation(job.getLocation() != null ? job.getLocation().trim() : null);
        job.setSalary(job.getSalary() != null ? job.getSalary().trim() : null);
        job.setRecruiterId(recruiterId);

        return jobRepository.save(job);
    }

    public List<JobListResponse> getAllJobs() {
        User currentUser = getOptionalCurrentUser();

        return jobRepository.findAll()
                .stream()
                .map(job -> toResponse(job, resolveMatchScore(currentUser, job)))
                .toList();
    }

    public Job getJobById(Long id) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        return jobRepository.findById(safeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found with id: " + safeId));
    }

    public List<Job> getMyJobs(String role, Long recruiterId) {
        if (!"RECRUITER".equals(role) || recruiterId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view their posted jobs");
        }

        return jobRepository.findByRecruiterId(recruiterId);
    }

    private User getOptionalCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private Double resolveMatchScore(User currentUser, Job job) {
        if (currentUser == null || currentUser.getRole() != Role.SEEKER) {
            return null;
        }

        return aiMatchingService.getMatchScore(currentUser.getResumeText(), job.getDescription());
    }

    private JobListResponse toResponse(Job job, Double matchScore) {
        return new JobListResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                job.getLocation(),
                job.getSalary(),
                job.getRecruiterId(),
                job.getCreatedAt(),
                matchScore
        );
    }
}
