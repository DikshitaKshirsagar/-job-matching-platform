package com.jobmatch.backend.repository;

import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.SavedJob;
import com.jobmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    Optional<SavedJob> findByUserAndJob(User user, Job job);
    List<SavedJob> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndJob(User user, Job job);
    void deleteByUserAndJob(User user, Job job);
}
