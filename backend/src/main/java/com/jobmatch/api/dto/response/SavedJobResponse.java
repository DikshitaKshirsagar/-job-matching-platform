package com.jobmatch.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class SavedJobResponse {
    private Long jobId;
    private String title;
    private String company;
    private String location;
    private String salary;
    private LocalDateTime savedAt;
    private Long recruiterId;
}
