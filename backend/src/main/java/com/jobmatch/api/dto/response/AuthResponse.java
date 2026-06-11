package com.jobmatch.api.dto.response;

import com.jobmatch.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private UserRole role;
    private Long userId;
    private String message;
}
