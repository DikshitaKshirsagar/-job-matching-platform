package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.AccountLockout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AccountLockoutRepository extends JpaRepository<AccountLockout, Long> {

    Optional<AccountLockout> findByEmail(String email);

    Optional<AccountLockout> findByIpAddress(String ipAddress);

    @Modifying
    @Query("DELETE FROM AccountLockout a WHERE a.lockedUntil < :now")
    void deleteExpiredLockouts(LocalDateTime now);

    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime after);

    @Modifying
    @Query("UPDATE AccountLockout a SET a.failedAttempts = 0, a.lockedUntil = NULL WHERE a.email = :email")
    void resetLockout(String email);
}