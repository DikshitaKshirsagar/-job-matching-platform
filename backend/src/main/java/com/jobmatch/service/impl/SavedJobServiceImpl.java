package com.jobmatch.service.impl;

import com.jobmatch.api.dto.response.SavedJobResponse;
import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.SavedJob;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.repository.JobRepository;
import com.jobmatch.domain.repository.SavedJobRepository;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.BadRequestException;
import com.jobmatch.exception.ResourceNotFoundException;
import com.jobmatch.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedJobServiceImpl implements SavedJobService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;

    @Override
    @Transactional
    public SavedJobResponse saveJob(Long jobId, Long userId) {
        log.info("User id: {} saving job id: {}", userId, jobId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Job job = jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (savedJobRepository.existsByUserAndJob(user, job)) {
            log.warn("Job id: {} already saved by user id: {}", jobId, userId);
            throw new BadRequestException("Job already saved");
        }

        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJob(job);

        SavedJob created = savedJobRepository.save(savedJob);
        log.info("Successfully saved job with id: {}", created.getId());
        return toResponse(created);
    }

    @Override
    @Transactional
    public void unsaveJob(Long jobId, Long userId) {
        log.info("User id: {} unsaving job id: {}", userId, jobId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (!savedJobRepository.existsByUserAndJob(user, job)) {
            throw new ResourceNotFoundException("Saved job not found");
        }

        savedJobRepository.deleteByUserAndJob(user, job);
        log.info("Successfully unsaved job with id: {}", jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedJobResponse> getSavedJobs(Long userId) {
        log.debug("Fetching saved jobs for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return savedJobRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SavedJobResponse toResponse(SavedJob savedJob) {
        Job job = savedJob.getJob();
        return SavedJobResponse.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .salary(job.getSalaryMin() != null ? job.getSalaryMin().toPlainString() : null)
                .savedAt(savedJob.getCreatedAt())
                .recruiterId(job.getRecruiter().getId())
                .build();
    }
}
