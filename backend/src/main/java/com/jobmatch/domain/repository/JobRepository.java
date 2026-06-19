package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Page<Job> findByStatusAndDeletedFalse(JobStatus status, Pageable pageable);

    List<Job> findByRecruiterIdAndDeletedFalse(Long recruiterId);

    Page<Job> findByRecruiterIdAndDeletedFalse(Long recruiterId, Pageable pageable);

    Optional<Job> findByIdAndDeletedFalse(Long id);

    @Query("""
        SELECT j FROM Job j
        WHERE j.deleted = false
        AND j.status = 'ACTIVE'
        AND (:keyword IS NULL OR
             LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:location IS NULL OR
             LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
        ORDER BY j.createdAt DESC
        """)
    Page<Job> searchJobs(
        @Param("keyword") String keyword,
        @Param("location") String location,
        Pageable pageable
    );

    // Portable search used by services that previously called the MySQL full-text query.
    @Query(value = """
        SELECT j FROM Job j
        WHERE j.deleted = false
        AND j.status = 'ACTIVE'
        AND (:keyword IS NULL OR
             LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:location IS NULL OR
             LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
        ORDER BY j.createdAt DESC
        """)
    Page<Job> searchJobsFulltext(
        @Param("keyword") String keyword,
        @Param("location") String location,
        Pageable pageable
    );

    // Uses composite index idx_job_recruiter_status
    Page<Job> findByRecruiterIdAndStatusAndDeletedFalse(Long recruiterId, JobStatus status, Pageable pageable);

    // Uses composite index idx_job_active_listing
    Page<Job> findByStatusAndDeletedFalseOrderByCreatedAtDesc(JobStatus status, Pageable pageable);

    long countByStatusAndDeletedFalse(JobStatus status);
}
