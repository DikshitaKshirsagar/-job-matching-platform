package com.jobmatch.service;

import com.jobmatch.api.dto.response.DashboardStatsResponse;
import com.jobmatch.api.dto.response.JobRecommendationResponse;
import com.jobmatch.api.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponse getUserProfile(Long userId);
    UserResponse updateUserProfile(Long userId, String name);
    void uploadResume(Long userId, MultipartFile file);
    UserResponse getCurrentUserProfile();
    DashboardStatsResponse getDashboard(Long userId);
    List<JobRecommendationResponse> getJobRecommendations(Long userId);
}
