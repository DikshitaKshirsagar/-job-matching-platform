package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.SavedJobResponse;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.SavedJob;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedJobService {

    private final UserService userService;
    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;

    public SavedJobResponse saveJob(Long jobId) {
        User user = userService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException("Job not found", HttpStatus.NOT_FOUND));

        if (savedJobRepository.existsByUserAndJob(user, job)) {
            throw new AppException("Job already saved", HttpStatus.BAD_REQUEST);
        }

        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJob(job);
        SavedJob created = savedJobRepository.save(savedJob);

        return toResponse(created);
    }

    public void unsaveJob(Long jobId) {
        User user = userService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException("Job not found", HttpStatus.NOT_FOUND));

        if (!savedJobRepository.existsByUserAndJob(user, job)) {
            throw new AppException("Saved job not found", HttpStatus.NOT_FOUND);
        }

        savedJobRepository.deleteByUserAndJob(user, job);
    }

    public List<SavedJobResponse> getSavedJobs() {
        User user = userService.getCurrentUser();
        return savedJobRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SavedJobResponse toResponse(SavedJob savedJob) {
        Job job = savedJob.getJob();
        return new SavedJobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getSalary(),
                savedJob.getCreatedAt(),
                job.getRecruiterId()
        );
    }
}
