package com.jobmatch.backend.dto;

import lombok.Data;

@Data
public class VerifyEmailRequest {
    private String token;
}
