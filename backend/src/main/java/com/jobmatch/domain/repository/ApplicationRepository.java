package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByApplicantIdAndDeletedFalse(Long applicantId);

    Page<Application> findByApplicantIdAndDeletedFalse(Long applicantId, Pageable pageable);

    Optional<Application> findByApplicantIdAndJobIdAndDeletedFalse(Long applicantId, Long jobId);

    Page<Application> findByJobIdAndDeletedFalse(Long jobId, Pageable pageable);

    @Query("SELECT a FROM Application a JOIN FETCH a.applicant WHERE a.job.id = :jobId AND a.deleted = false")
    List<Application> findByJobIdWithApplicant(@Param("jobId") Long jobId);

    long countByJobIdAndDeletedFalse(Long jobId);

    long countByStatus(String status);

    long count();

    @Query("SELECT COUNT(a) FROM Application a WHERE a.status = :status AND a.createdAt BETWEEN :start AND :end")
    long countByStatusAndCreatedAtBetween(@Param("status") String status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.createdAt BETWEEN :start AND :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Backward compatibility with existing service implementations
    boolean existsByApplicantIdAndJobId(Long applicantId, Long jobId);

    // Used by existing ApplicationServiceImpl
    List<Application> findByApplicantOrderByCreatedAtDesc(Long applicantId);

    // Used by existing ApplicationServiceImpl
    Page<Application> findByJobOrderByMatchScoreDesc(Long jobId, Pageable pageable);

    // Used by UserServiceImpl
    long countByApplicantId(Long applicantId);
}