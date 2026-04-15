package com.jobmatch.backend.service;

import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    // POST - Create a new job (Recruiter only)
    public Job createJob(Job job, String role, Long recruiterId) {
        if (!role.equals("RECRUITER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can post jobs");
        }
        job.setRecruiterId(recruiterId);   // matches your Job.java field
        return jobRepository.save(job);
    }

    // GET - All jobs (any logged-in user)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    // GET - Single job by ID
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found with id: " + id));
    }

    // GET - Jobs posted by logged-in recruiter
    public List<Job> getMyJobs(String role, Long recruiterId) {
        if (!role.equals("RECRUITER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view their posted jobs");
        }
        return jobRepository.findByRecruiterId(recruiterId);  // matches your Job.java field
    }
}