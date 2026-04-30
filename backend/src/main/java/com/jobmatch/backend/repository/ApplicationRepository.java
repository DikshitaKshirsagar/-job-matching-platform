package com.jobmatch.backend.repository;

import com.jobmatch.backend.entity.Application;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUser(User user);

    long countByUser(User user);   // ✅ FIX ADDED

    List<Application> findByJob(Job job);

    List<Application> findByJobOrderByMatchScoreDesc(Job job);

    boolean existsByUserAndJob(User user, Job job);
}