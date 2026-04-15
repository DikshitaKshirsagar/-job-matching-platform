package com.jobmatch.backend.repository;

import com.jobmatch.backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiterId(Long recruiterId);  // matches your Job.java field
}