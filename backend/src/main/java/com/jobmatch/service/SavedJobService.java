package com.jobmatch.service;

import com.jobmatch.api.dto.response.SavedJobResponse;

import java.util.List;

public interface SavedJobService {
    SavedJobResponse saveJob(Long jobId, Long userId);
    void unsaveJob(Long jobId, Long userId);
    List<SavedJobResponse> getSavedJobs(Long userId);
}
