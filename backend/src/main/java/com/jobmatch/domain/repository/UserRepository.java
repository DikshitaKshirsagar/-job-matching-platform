package com.jobmatch.domain.repository;

import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
