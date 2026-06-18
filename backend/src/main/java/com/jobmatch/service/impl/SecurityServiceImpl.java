package com.jobmatch.service.impl;

import com.jobmatch.domain.entity.*;
import com.jobmatch.domain.repository.*;
import com.jobmatch.service.NotificationService;
import com.jobmatch.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityServiceImpl implements SecurityService {

    private final AccountLockoutRepository accountLockoutRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final NotificationService notificationService;
    private final HttpServletRequest httpServletRequest;

    @Value("${app.security.account-lockout-threshold:5}")
    private int lockoutThreshold;

    @Value("${app.security.account-lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    // Password policy patterns
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    // OWASP SQL injection patterns to sanitize
    private static final Pattern SQL_INJECTION_PATTERN =
            Pattern.compile("(?i)\\b(select|insert|update|delete|drop|alter|truncate|union|exec|execute)\\b");

    @Override
    @Transactional
    public boolean recordLoginAttempt(String email, String ipAddress, boolean success, Long userId) {
        log.debug("Recording login attempt for email: {} from IP: {} - success: {}", email, ipAddress, success);

        // Record the attempt
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setIpAddress(ipAddress);
        attempt.setSuccess(success);
        attempt.setAttemptTime(LocalDateTime.now());
        attempt.setUserId(userId);

        if (success) {
            resetLockout(email);
            return true;
        }

        // Check or create lockout record
        AccountLockout lockout = accountLockoutRepository.findByEmail(email)
                .orElseGet(() -> {
                    AccountLockout newLockout = new AccountLockout();
                    newLockout.setEmail(email);
                    newLockout.setIpAddress(ipAddress);
                    newLockout.setUserId(userId);
                    newLockout.setFailedAttempts(0);
                    return newLockout;
                });

        lockout.setFailedAttempts(lockout.getFailedAttempts() + 1);
        lockout.setUpdatedAt(LocalDateTime.now());

        if (lockout.getFailedAttempts() >= lockoutThreshold) {
            lockout.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            log.warn("Account locked for email: {} due to {} failed attempts", email, lockout.getFailedAttempts());

            // Send notification
            notificationService.createNotification(userId, "SECURITY_ALERT",
                    "Account Locked",
                    "Your account has been temporarily locked due to multiple failed login attempts. " +
                    "Please try again in " + lockoutDurationMinutes + " minutes.",
                    "System", null);

            return false;
        }

        accountLockoutRepository.save(lockout);
        return false;
    }

    @Override
    public boolean isAccountLocked(String email, String ipAddress) {
        Optional<AccountLockout> lockout = accountLockoutRepository.findByEmail(email);
        if (lockout.isPresent() && lockout.get().isLocked()) {
            log.warn("Account locked for email: {}", email);
            return true;
        }

        // Also check IP-based rate limiting
        long attemptsFromIp = accountLockoutRepository
                .countByIpAddressAndCreatedAtAfter(ipAddress, LocalDateTime.now().minusHours(1));
        return attemptsFromIp >= 20;
    }

    @Override
    @Transactional
    public void resetLockout(String email) {
        accountLockoutRepository.resetLockout(email);
    }

    @Override
    public String validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long";
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return "Password must contain at least one uppercase letter";
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return "Password must contain at least one lowercase letter";
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            return "Password must contain at least one number";
        }
        if (!SPECIAL_PATTERN.matcher(password).find()) {
            return "Password must contain at least one special character";
        }
        return null; // Password is valid
    }

    @Override
    public void logAuditEvent(Long userId, String action, String entityType, Long entityId,
                              String details, String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress != null ? ipAddress : httpServletRequest.getRemoteAddr());
        auditLog.setUserAgent(userAgent != null ? userAgent : httpServletRequest.getHeader("User-Agent"));
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
        log.debug("Audit log saved: {} - {} - {}", userId, action, entityType);
    }

    @Override
    @Transactional
    public void requestDataDeletion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete user data
        user.setName("Deleted User");
        user.setEmail("deleted-" + userId + "@anonymized.com");
        user.setPassword(UUID.randomUUID().toString());
        user.setResumeText(null);
        user.setResumeFileName(null);
        user.setRefreshToken(null);
        user.setVerificationToken(null);
        user.setPasswordResetToken(null);
        user.setDeleted(true);
        userRepository.save(user);

        log.info("GDPR data deletion completed for user: {}", userId);
    }

    @Override
    @Transactional
    public void requestDataExport(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("GDPR data export initiated for user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("profile", Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "createdAt", user.getCreatedAt(),
                "hasResume", user.getResumeText() != null
        ));
        data.put("applications", applicationRepository.countByApplicantId(userId));
        data.put("savedJobs", savedJobRepository.countByUserId(userId));
        data.put("auditLogs", auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getSize());

        return data;
    }

    @Override
    public String sanitizeInput(String input) {
        if (input == null) return null;
        // Remove SQL injection patterns
        String sanitized = SQL_INJECTION_PATTERN.matcher(input).replaceAll("");
        // Remove potential XSS
        sanitized = sanitized.replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("on\\w+\\s*=", "")
                .replaceAll("javascript\\s*:", "");
        return sanitized.trim();
    }
}