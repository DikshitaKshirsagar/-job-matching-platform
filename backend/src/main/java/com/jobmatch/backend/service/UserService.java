package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.DashboardResponse;
import com.jobmatch.backend.dto.ResumeUploadResponse;
import com.jobmatch.backend.dto.UserProfileResponse;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.exception.AppException;
import com.jobmatch.backend.repository.ApplicationRepository;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

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

    // ✅ Get current logged-in user (FIXED)
    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // ❗ VERY IMPORTANT CHECKS
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();

        if (email == null || email.equals("anonymousUser")) {
            throw new AppException("Invalid user", HttpStatus.UNAUTHORIZED);
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    // ✅ Get user profile
    public UserProfileResponse getUserProfile() {

        User user = getCurrentUser();

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getResumeFileName(),
                user.getResumeText() != null && !user.getResumeText().trim().isEmpty(),
                user.getCreatedAt()
        );
    }

    // ✅ Get dashboard data
    public DashboardResponse getDashboard() {
        User user = getCurrentUser();
        long totalApplications = applicationRepository.findByUserId(user.getId()).size();
        long totalJobs = jobRepository.count();
        boolean resumeUploaded = user.getResumeText() != null && !user.getResumeText().trim().isEmpty();
        return new DashboardResponse(
                user.getName(),
                user.getEmail(),
                totalApplications,
                totalJobs,
                resumeUploaded
        );
    }

    // ✅ Upload Resume API
    public ResumeUploadResponse uploadResume(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException("File is empty", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.contains("pdf")) {
            throw new AppException("Only PDF files are allowed", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new AppException("File size exceeds 10MB limit", HttpStatus.BAD_REQUEST);
        }

        User user = getCurrentUser();

        try {
            String extractedText = extractTextFromPDF(file);

            user.setResumeText(extractedText);
            user.setResumeFileName(file.getOriginalFilename());

            userRepository.save(user);

            log.info("Resume uploaded successfully for user: {}", user.getEmail());

            return new ResumeUploadResponse(
                    "Resume uploaded successfully",
                    file.getOriginalFilename(),
                    file.getSize(),
                    true
            );

        } catch (IOException e) {

            log.error("Error processing PDF for user: {}", user.getEmail(), e);

            throw new AppException(
                    "Error processing PDF file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // ✅ Extract text from PDF
    private String extractTextFromPDF(MultipartFile file) throws IOException {

        byte[] pdfBytes = file.getBytes();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new IOException("PDF contains no readable text");
            }

            return text;
        }
    }
}
