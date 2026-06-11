package com.jobmatch.service.impl;

import com.jobmatch.api.dto.request.CreateJobRequest;
import com.jobmatch.api.dto.request.UpdateJobRequest;
import com.jobmatch.api.dto.response.JobResponse;
import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.JobStatus;
import com.jobmatch.domain.enums.JobType;
import com.jobmatch.domain.repository.JobRepository;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ResourceNotFoundException;
import com.jobmatch.exception.UnauthorizedException;
import com.jobmatch.service.JobService;
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
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request, Long recruiterId) {
        log.info("Creating job '{}' for recruiter id: {}", request.getTitle(), recruiterId);

        if (request == null) {
            throw new BadRequestException("Job request cannot be null");
        }

        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", recruiterId));

        Job job = new Job();
        job.setTitle(request.getTitle().trim());
        job.setDescription(request.getDescription().trim());
        job.setCompany(request.getCompany().trim());
        job.setLocation(request.getLocation().trim());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setRecruiter(recruiter);
        job.setStatus(JobStatus.ACTIVE);

        if (request.getJobType() != null) {
            try {
                job.setJobType(JobType.valueOf(request.getJobType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid job type: " + request.getJobType());
            }
        }

        if (request.getSalaryMin() != null) {
            job.setSalaryMin(request.getSalaryMin());
        }
        if (request.getSalaryMax() != null) {
            job.setSalaryMax(request.getSalaryMax());
        }

        Job savedJob = jobRepository.save(job);
        log.info("Successfully created job with id: {}", savedJob.getId());
        return toResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        log.debug("Fetching job with id: {}", id);

        Job job = jobRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));

        return toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(String keyword, String location, Pageable pageable) {
        log.debug("Searching jobs with keyword: {}, location: {}", keyword, location);
        return jobRepository.searchJobs(keyword, location, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllActiveJobs(Pageable pageable) {
        log.debug("Fetching all active jobs");
        return jobRepository.findByStatusAndDeletedFalse(JobStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public JobResponse updateJob(Long id, UpdateJobRequest request, Long recruiterId) {
        log.info("Updating job id: {} by recruiter id: {}", id, recruiterId);

        Job job = jobRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));

        if (!job.getRecruiter().getId().equals(recruiterId)) {
            log.warn("Unauthorized job update attempt for job id: {} by recruiter id: {}", id, recruiterId);
            throw new UnauthorizedException("You can only update your own jobs");
        }

        if (request.getTitle() != null) {
            job.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            job.setDescription(request.getDescription().trim());
        }
        if (request.getCompany() != null) {
            job.setCompany(request.getCompany().trim());
        }
        if (request.getLocation() != null) {
            job.setLocation(request.getLocation().trim());
        }
        if (request.getRequiredSkills() != null) {
            job.setRequiredSkills(request.getRequiredSkills());
        }
        if (request.getSalaryMin() != null) {
            job.setSalaryMin(request.getSalaryMin());
        }
        if (request.getSalaryMax() != null) {
            job.setSalaryMax(request.getSalaryMax());
        }

        Job updated = jobRepository.save(job);
        log.info("Successfully updated job with id: {}", updated.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteJob(Long id, Long recruiterId) {
        log.info("Deleting job id: {} by recruiter id: {}", id, recruiterId);

        Job job = jobRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));

        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new UnauthorizedException("You can only delete your own jobs");
        }

        job.setDeleted(true);
        jobRepository.save(job);
        log.info("Successfully deleted job with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByRecruiter(Long recruiterId) {
        log.debug("Fetching jobs for recruiter id: {}", recruiterId);
        return jobRepository.findByRecruiterIdAndDeletedFalse(recruiterId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobRecommendations(Long userId) {
        log.debug("Fetching job recommendations for user id: {}", userId);
        // Placeholder implementation - should integrate with AI matching service
        return getAllActiveJobs(Pageable.unpaged()).getContent();
    }

    private JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .recruiterId(job.getRecruiter().getId())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
