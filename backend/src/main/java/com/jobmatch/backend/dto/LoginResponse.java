package com.jobmatch.backend.dto;

import com.jobmatch.backend.entity.Role;

public record LoginResponse(
        String token,
        String name,
        String email,
        Role role,
        Long userId,
        String message
) {
}
