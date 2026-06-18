package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.BatchJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {
}