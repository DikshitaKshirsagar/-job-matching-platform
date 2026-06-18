package com.jobmatch.service;

import java.util.Map;

/**
 * Service interface for security operations including account lockout,
 * password policy, audit logging, and GDPR compliance.
 */
public interface SecurityService {

    /**
     * Record a login attempt and check if account should be locked.
     * @return true if login is allowed, false if account is locked
     */
    boolean recordLoginAttempt(String email, String ipAddress, boolean success, Long userId);

    /**
     * Check if an account is currently locked.
     */
    boolean isAccountLocked(String email, String ipAddress);

    /**
     * Reset lockout counter after successful login.
     */
    void resetLockout(String email);

    /**
     * Validate password meets policy requirements.
     * @return null if valid, error message if invalid
     */
    String validatePassword(String password);

    /**
     * Log an audit event.
     */
    void logAuditEvent(Long userId, String action, String entityType, Long entityId,
                       String details, String ipAddress, String userAgent);

    /**
     * Initiate GDPR data deletion request.
     */
    void requestDataDeletion(Long userId);

    /**
     * Initiate GDPR data export request.
     */
    void requestDataExport(Long userId);

    /**
     * Export user data for GDPR compliance.
     */
    Map<String, Object> exportUserData(Long userId);

    /**
     * Perform OWASP input sanitization.
     */
    String sanitizeInput(String input);
}