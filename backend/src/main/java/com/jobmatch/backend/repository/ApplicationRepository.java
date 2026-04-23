package com.jobmatch.backend.repository;

import com.jobmatch.backend.entity.Application;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // ✅ Get applications of a user
    List<Application> findByUser(User user);

    // ✅ Get applications for a job (NORMAL)
    List<Application> findByJob(Job job);

    // ✅ Get applications sorted by matchScore (IMPORTANT 🔥)
    List<Application> findByJobOrderByMatchScoreDesc(Job job);

    // ✅ Prevent duplicate applications
    boolean existsByUserAndJob(User user, Job job);
}