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

    // ✅ Get current logged-in user
    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    // ✅ Get user profile (MATCH DTO)
    public UserProfileResponse getUserProfile() {

        User user = getCurrentUser();

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                null, // resumeFileName - add if needed
                user.getResumeText() != null && !user.getResumeText().isBlank(),
                user.getCreatedAt() // assume exists
        );
    }

    // ✅ Dashboard
    public DashboardResponse getDashboard() {

        User user = getCurrentUser();

        long totalApplications = applicationRepository.findByUser(user).size();
        long totalJobs = jobRepository.count();
        boolean resumeUploaded = user.getResumeText() != null && !user.getResumeText().isBlank();

        return new DashboardResponse(
                user.getName(),
                user.getEmail(),
                totalApplications,
                totalJobs,
                resumeUploaded
        );
    }

    // ✅ Upload Resume (CORRECTED)
    public ResumeUploadResponse uploadResume(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException("No file provided", HttpStatus.BAD_REQUEST);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new AppException("Only PDF files (.pdf) are allowed", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new AppException("Invalid PDF content type: " + contentType, HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > maxFileSize) {
            throw new AppException("File size " + file.getSize() + " exceeds " + (maxFileSize/1024/1024) + "MB limit", HttpStatus.BAD_REQUEST);
        }

        User user = getCurrentUser();

        try {
            String extractedText = extractTextFromPDF(file);
            String trimmedText = extractedText.trim();
            if (trimmedText.length() > maxTextLength) {
                trimmedText = trimmedText.substring(0, maxTextLength);
                log.warn("Resume text truncated to {} chars for user {}", maxTextLength, user.getEmail());
            }

            user.setResumeText(trimmedText);
            userRepository.save(user);

            log.info("Resume uploaded: {} ({} bytes, {} chars) for {}", filename, file.getSize(), trimmedText.length(), user.getEmail());

            return new ResumeUploadResponse(
                    "Resume uploaded successfully",
                    filename,
                    file.getSize(),
                    true
            );

        } catch (IOException e) {
            log.error("PDF processing failed for {}: {}", filename, e.getMessage(), e);
            throw new AppException("Failed to extract text from PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ✅ Extract text from PDF
    private String extractTextFromPDF(MultipartFile file) throws IOException {

        byte[] pdfBytes = file.getBytes();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new IOException("PDF contains no extractable text (scanned image?)");
            }

            return text;
        }
    }
}
