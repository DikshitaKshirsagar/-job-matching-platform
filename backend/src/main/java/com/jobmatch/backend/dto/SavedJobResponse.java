package com.jobmatch.backend.dto;

import java.time.LocalDateTime;

public class SavedJobResponse {
    private Long jobId;
    private String title;
    private String company;
    private String location;
    private String salary;
    private LocalDateTime savedAt;
    private Long recruiterId;

    public SavedJobResponse() {
    }

    public SavedJobResponse(Long jobId, String title, String company, String location, String salary, LocalDateTime savedAt, Long recruiterId) {
        this.jobId = jobId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.savedAt = savedAt;
        this.recruiterId = recruiterId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public Long getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(Long recruiterId) {
        this.recruiterId = recruiterId;
    }
}
