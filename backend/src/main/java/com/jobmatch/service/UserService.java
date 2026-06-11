package com.jobmatch.service;

import com.jobmatch.api.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponse getUserProfile(Long userId);
    UserResponse updateUserProfile(Long userId, String name);
    void uploadResume(Long userId, MultipartFile file);
    UserResponse getCurrentUserProfile();
}
