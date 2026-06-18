package com.jobmatch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_login_email", columnList = "email"),
    @Index(name = "idx_login_ip", columnList = "ip_address"),
    @Index(name = "idx_login_time", columnList = "attempt_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(nullable = false)
    private boolean success = false;
}