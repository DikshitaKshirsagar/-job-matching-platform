package com.jobmatch.backend.repository;

import com.jobmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Find user by email
    Optional<User> findByEmail(String email);

    // ✅ Check if user exists
    boolean existsByEmail(String email);

    // Optional features (keep if used)
    Optional<User> findByVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);
}