package com.jobmatch.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendVerificationEmail(String to, String token, String userName) {
        String subject = "Verify your JobMatch email";
        String verificationUrl = "http://localhost:3000/verify-email?token=" + token;
        String body = """
            Hi %s,
            
            Thank you for registering at JobMatch!
            
            Please click the link below to verify your email:
            %s
            
            This link expires in 24 hours.
            
            Best,
            JobMatch Team
            """.formatted(userName, verificationUrl);
        
        log.info("MOCK VERIFICATION EMAIL - Subject: '{}', Body preview: '{}' to {}", subject, body.substring(0, Math.min(100, body.length())), to);
        // Mock send - log instead of real email
    }

    public void sendPasswordResetEmail(String to, String token, String userName) {
        String subject = "Reset your JobMatch password";
        String resetUrl = "http://localhost:3000/reset-password?token=" + token;
        String body = """
            Hi %s,
            
            You requested a password reset.
            
            Click the link to reset:
            %s
            
            This expires in 1 hour.
            
            If you didn't request this, ignore.
            
            JobMatch Team
            """.formatted(userName, resetUrl);
        
        log.info("MOCK PASSWORD RESET EMAIL - Subject: '{}', Body preview: '{}' to {}", subject, body.substring(0, Math.min(100, body.length())), to);
        // Mock send
    }
}

