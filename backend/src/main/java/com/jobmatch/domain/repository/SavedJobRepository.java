package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.SavedJob;
import com.jobmatch.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    Optional<SavedJob> findByUserAndJob(User user, Job job);

    List<SavedJob> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndJob(User user, Job job);

    void deleteByUserAndJob(User user, Job job);

    long countByUserId(Long userId);
}
