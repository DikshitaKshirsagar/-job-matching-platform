package com.jobmatch.api.dto.response;

import com.jobmatch.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private String resumeFileName;
    private boolean hasResume;
    private LocalDateTime createdAt;
}
