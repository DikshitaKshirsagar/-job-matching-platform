package com.jobmatch.backend.repository;

import com.jobmatch.backend.entity.Application;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUser(User user);
    Page<Application> findByUser(User user, Pageable pageable);

    long countByUser(User user);   // ✅ FIX ADDED

    List<Application> findByJob(Job job);
    Page<Application> findByJobOrderByMatchScoreDesc(Job job, Pageable pageable);

    boolean existsByUserAndJob(User user, Job job);
}