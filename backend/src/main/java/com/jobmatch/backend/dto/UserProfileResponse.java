package com.jobmatch.backend.dto;

import com.jobmatch.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String resumeFileName;
    private boolean hasResume;
    private LocalDateTime createdAt;
}