package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.*;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    @Value("${app.resume.max-size:10485760}")
    private long maxFileSize;

    @Value("${app.resume.max-text-length:100000}")
    private int maxTextLength;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() ||
                auth.getPrincipal() == null ||
                "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String email = auth.getName().trim().toLowerCase();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    public UserProfileResponse getUserProfile() {
        User user = getCurrentUser();

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getResumeFileName(),
                user.getResumeText() != null && !user.getResumeText().isBlank(),
                user.getCreatedAt()
        );
    }

    public DashboardResponse getDashboard() {
        User user = getCurrentUser();

        return new DashboardResponse(
                user.getName(),
                user.getEmail(),
                applicationRepository.countByUser(user),
                jobRepository.count(),
                user.getResumeText() != null && !user.getResumeText().isBlank()
        );
    }

    public ResumeUploadResponse uploadResume(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException("No file provided", HttpStatus.BAD_REQUEST);
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new AppException("Only PDF allowed", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > maxFileSize) {
            throw new AppException("File too large", HttpStatus.BAD_REQUEST);
        }

        User user = getCurrentUser();

        try {
            String text = extractTextFromPDF(file).trim();

            if (text.length() > maxTextLength) {
                text = text.substring(0, maxTextLength);
            }

            user.setResumeText(text);
            user.setResumeFileName(filename);
            userRepository.save(user);

            return new ResumeUploadResponse(
                    "Resume uploaded successfully",
                    filename,
                    file.getSize(),
                    true
            );

        } catch (Exception e) {
            throw new AppException("PDF processing failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            if (text == null || text.trim().isEmpty()) {
                throw new IOException("Empty PDF");
            }

            return text;
        }
    }
}
