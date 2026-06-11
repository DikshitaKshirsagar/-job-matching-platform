package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.Application;
import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByApplicantIdAndDeletedFalse(Long applicantId);

    Page<Application> findByApplicantIdAndDeletedFalse(Long applicantId, Pageable pageable);

    List<Application> findByJobId(Long jobId);

    Optional<Application> findByApplicantIdAndJobId(Long applicantId, Long jobId);

    boolean existsByApplicantIdAndJobId(Long applicantId, Long jobId);

    Page<Application> findByJobRecruiterIdAndDeletedFalse(Long recruiterId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.id = :jobId AND a.deleted = false")
    long countByJobIdAndDeletedFalse(@Param("jobId") Long jobId);

    long countByApplicantId(Long applicantId);

    @Query("""
        SELECT a FROM Application a
        WHERE a.deleted = false
        AND a.applicant.id = :applicantId
        ORDER BY a.createdAt DESC
        """)
    List<Application> findByApplicantOrderByCreatedAtDesc(@Param("applicantId") Long applicantId);

    @Query("""
        SELECT a FROM Application a
        WHERE a.deleted = false
        AND a.job.id = :jobId
        ORDER BY a.matchScore DESC
        """)
    Page<Application> findByJobOrderByMatchScoreDesc(@Param("jobId") Long jobId, Pageable pageable);
}
