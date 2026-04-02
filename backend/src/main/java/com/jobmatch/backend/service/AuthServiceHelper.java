package com.jobmatch.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuthServiceHelper {

    private static final long VERIFICATION_EXPIRY_HOURS = 24;
    private static final long RESET_EXPIRY_HOURS = 1;

    public static String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    public static String generateResetToken() {
        return UUID.randomUUID().toString();
    }

    public static LocalDateTime getVerificationExpiry() {
        return LocalDateTime.now().plusHours(VERIFICATION_EXPIRY_HOURS);
    }

    public static LocalDateTime getResetExpiry() {
        return LocalDateTime.now().plusHours(RESET_EXPIRY_HOURS);
    }

public static boolean isVerificationTokenValid(String token, LocalDateTime expiry) {
        return token != null && expiry != null && LocalDateTime.now().isBefore(expiry);
    }

public static boolean isResetTokenValid(String token, LocalDateTime expiry) {
        return token != null && expiry != null && LocalDateTime.now().isBefore(expiry);
    }
}
