package com.jobmatch.service;

import com.jobmatch.api.dto.request.CreateJobRequest;
import com.jobmatch.api.dto.request.UpdateJobRequest;
import com.jobmatch.api.dto.response.JobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobService {
    JobResponse createJob(CreateJobRequest request, Long recruiterId);
    JobResponse getJobById(Long id);
    Page<JobResponse> searchJobs(String keyword, String location, Pageable pageable);
    Page<JobResponse> getAllActiveJobs(Pageable pageable);
    JobResponse updateJob(Long id, UpdateJobRequest request, Long recruiterId);
    void deleteJob(Long id, Long recruiterId);
    List<JobResponse> getJobsByRecruiter(Long recruiterId);
    List<JobResponse> getJobRecommendations(Long userId);
}
