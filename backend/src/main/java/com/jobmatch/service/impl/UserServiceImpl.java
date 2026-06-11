package com.jobmatch.service.impl;

import com.jobmatch.api.dto.response.UserResponse;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.FileUploadException;
import com.jobmatch.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements com.jobmatch.service.UserService {

    private final UserRepository userRepository;

    @Value("${app.resume.max-size:10485760}")
    private long maxFileSize;

    @Value("${app.resume.max-text-length:100000}")
    private int maxTextLength;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        log.debug("Fetching profile for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(Long userId, String name) {
        log.info("Updating profile for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        user.setName(name.trim());
        userRepository.save(user);
        log.info("Successfully updated profile for user id: {}", userId);

        return toResponse(user);
    }

    @Override
    @Transactional
    public void uploadResume(Long userId, MultipartFile file) {
        log.info("Uploading resume for user id: {}", userId);

        if (file == null || file.isEmpty()) {
            throw new FileUploadException("No file provided");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new FileUploadException("Only PDF files are allowed");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileUploadException("File size exceeds maximum allowed limit");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        try {
            String text = extractTextFromPDF(file);

            if (text.length() > maxTextLength) {
                text = text.substring(0, maxTextLength);
            }

            user.setResumeText(text);
            user.setResumeFileName(filename);
            userRepository.save(user);
            log.info("Successfully uploaded resume for user id: {}", userId);

        } catch (IOException e) {
            log.error("PDF processing failed: {}", e.getMessage());
            throw new FileUploadException("PDF processing failed");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        log.debug("Fetching current user profile");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toResponse(user);
    }

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            if (text == null || text.trim().isEmpty()) {
                throw new IOException("PDF is empty");
            }

            return text;
        }
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .resumeFileName(user.getResumeFileName())
                .hasResume(user.getResumeText() != null && !user.getResumeText().isBlank())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
