package com.jobmatch.backend.repository;

import com.jobmatch.backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByRecruiterId(Long recruiterId);  // matches your Job.java field
    Page<Job> findByRecruiterId(Long recruiterId, Pageable pageable);
}