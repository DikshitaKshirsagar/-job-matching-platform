package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.ResumeUploadResponse;
import com.jobmatch.backend.dto.UserProfileResponse;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import com.jobmatch.backend.exception.AppException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

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

    public ResumeUploadResponse uploadResume(MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
throw new AppException("File is empty", HttpStatus.BAD_REQUEST);
        }

        // Validate file type
        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType)) {
throw new AppException("Only PDF files are allowed", HttpStatus.BAD_REQUEST);
        }

        // Validate file size (10MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
throw new AppException("File size exceeds 10MB limit", HttpStatus.BAD_REQUEST);
        }

        User user = getCurrentUser();

        try {
            // Extract text from PDF
            String extractedText = extractTextFromPDF(file);

            // Update user with resume data
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
            log.error("Error processing PDF file for user: {}", user.getEmail(), e);
throw new AppException("Error processing PDF file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        byte[] pdfBytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}