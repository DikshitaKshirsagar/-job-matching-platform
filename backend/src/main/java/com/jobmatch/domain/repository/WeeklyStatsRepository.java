package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.WeeklyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyStatsRepository extends JpaRepository<WeeklyStats, Long> {

    Optional<WeeklyStats> findByWeekStart(LocalDate weekStart);
}