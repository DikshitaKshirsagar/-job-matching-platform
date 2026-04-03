package com.jobmatch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadResponse {
    private String message;
    private String fileName;
    private Long fileSize;
    private boolean success;
}