package com.jobmatch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    private String name;
    private String email;
    private long totalApplications;
    private long totalJobs;
    private boolean resumeUploaded;
}
