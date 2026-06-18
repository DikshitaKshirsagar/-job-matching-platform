package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    Optional<User> findFirstByOrderByIdAsc();

    Optional<User> findFirstByRoleOrderByIdAsc(UserRole role);

    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT u FROM User u WHERE u.verificationTokenExpiry IS NOT NULL AND u.verificationTokenExpiry < :cutoff")
    List<User> findByVerificationTokenExpiryBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT u FROM User u WHERE u.passwordResetTokenExpiry IS NOT NULL AND u.passwordResetTokenExpiry < :cutoff")
    List<User> findByPasswordResetTokenExpiryBefore(@Param("cutoff") LocalDateTime cutoff);

    long countByDeletedFalse();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false AND u.role = 'ROLE_RECRUITER'")
    long countByRoleAndDeletedFalse(@Param("role") String role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.createdAt BETWEEN :start AND :end")
    long countByRoleAndCreatedAtBetween(@Param("role") String role, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByDeletedFalseAndRole(UserRole role);
}