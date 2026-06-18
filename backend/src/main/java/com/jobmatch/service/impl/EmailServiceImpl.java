package com.jobmatch.service.impl;

import com.jobmatch.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@jobmatch.com}")
    private String fromAddress;

    @Override
    public void sendVerificationEmail(String to, String token) {
        String subject = "Email Verification - JobMatch Platform";
        String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        String text = "Please verify your email by clicking the link below:\n\n"
                + verificationUrl + "\n\n"
                + "This link will expire in 24 hours.\n\n"
                + "If you did not create an account, please ignore this email.";

        sendEmail(to, subject, text);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Password Reset - JobMatch Platform";
        String resetUrl = baseUrl + "/api/v1/auth/reset-password?token=" + token;
        String text = "You have requested a password reset. Click the link below to reset your password:\n\n"
                + resetUrl + "\n\n"
                + "This link will expire in 1 hour.\n\n"
                + "If you did not request a password reset, please ignore this email.";

        sendEmail(to, subject, text);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            // Don't throw - email failures shouldn't block the main flow
        }
    }
}