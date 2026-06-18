package com.jobmatch.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DashboardStatsResponse {
    private UserResponse user;
    private long applicationsCount;
    private long savedJobsCount;
    private long matchedJobs;
}