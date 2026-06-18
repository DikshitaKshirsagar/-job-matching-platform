package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {

    Optional<MonthlyReport> findByMonthYear(String monthYear);
}