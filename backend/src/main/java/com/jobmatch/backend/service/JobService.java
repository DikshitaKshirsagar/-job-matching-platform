package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.JobListResponse;
import com.jobmatch.backend.dto.JobRequest;
import com.jobmatch.backend.dto.JobResponse;
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

    public JobResponse createJob(JobRequest request, String role, Long recruiterId) {
        if (!"RECRUITER".equals(role) || recruiterId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can post jobs");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job payload is required");
        }

        Job job = new Job();
        job.setTitle(request.title().trim());
        job.setCompany(request.company().trim());
        job.setDescription(request.description().trim());
        job.setLocation(request.location() != null ? request.location().trim() : null);
        job.setSalary(request.salary() != null ? request.salary().trim() : null);
        job.setRecruiterId(recruiterId);

        return toJobResponse(jobRepository.save(job));
    }

    public List<JobListResponse> getAllJobs() {
        User currentUser = getOptionalCurrentUser();

        return jobRepository.findAll()
                .stream()
                .map(job -> toResponse(job, resolveMatchScore(currentUser, job)))
                .toList();
    }

    public JobResponse getJobById(Long id) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        return jobRepository.findById(safeId)
                .map(this::toJobResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found with id: " + safeId));
    }

    public List<JobResponse> getMyJobs(String role, Long recruiterId) {
        if (!"RECRUITER".equals(role) || recruiterId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view their posted jobs");
        }

        return jobRepository.findByRecruiterId(recruiterId)
                .stream()
                .map(this::toJobResponse)
                .toList();
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

    private JobResponse toJobResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                job.getLocation(),
                job.getSalary(),
                job.getRecruiterId(),
                job.getCreatedAt()
        );
    }
}
