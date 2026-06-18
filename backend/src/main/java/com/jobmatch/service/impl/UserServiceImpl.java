package com.jobmatch.service.impl;

import com.jobmatch.api.dto.response.DashboardStatsResponse;
import com.jobmatch.api.dto.response.JobRecommendationResponse;
import com.jobmatch.api.dto.response.JobResponse;
import com.jobmatch.api.dto.response.UserResponse;
import com.jobmatch.domain.entity.Job;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.JobStatus;
import com.jobmatch.domain.repository.ApplicationRepository;
import com.jobmatch.domain.repository.JobRepository;
import com.jobmatch.domain.repository.SavedJobRepository;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.FileUploadException;
import com.jobmatch.exception.ResourceNotFoundException;
import com.jobmatch.infrastructure.external.AiServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements com.jobmatch.service.UserService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final AiServiceClient aiServiceClient;

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

        // Validate MIME type using Apache Tika to prevent spoofing
        try {
            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());
            log.debug("Detected MIME type: {} for file: {}", mimeType, filename);
            if (!"application/pdf".equals(mimeType)) {
                log.warn("Rejected file upload with detected MIME type: {} for user id: {}", mimeType, userId);
                throw new FileUploadException("Only PDF files are allowed");
            }
        } catch (IOException e) {
            log.error("MIME type detection failed: {}", e.getMessage());
            throw new FileUploadException("File validation failed");
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "jobRecommendations", key = "#userId", unless = "#result.isEmpty()")
    public List<JobRecommendationResponse> getJobRecommendations(Long userId) {
        log.info("Generating AI job recommendations for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String resumeText = user.getResumeText();
        if (resumeText == null || resumeText.isBlank()) {
            log.warn("User {} has no resume uploaded; returning empty recommendations", userId);
            return List.of();
        }

        // Fetch top 50 active jobs ordered by created date
        Page<Job> jobsPage = jobRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(
                JobStatus.ACTIVE, PageRequest.of(0, 50));
        List<Job> activeJobs = jobsPage.getContent();

        if (activeJobs.isEmpty()) {
            log.info("No active jobs found for recommendations");
            return List.of();
        }

        // For each job, call AI /match endpoint with resume vs job description
        List<JobRecommendationResponse> recommendations = new ArrayList<>();
        for (Job job : activeJobs) {
            String jobDescription = job.getDescription() != null ? job.getDescription() : "";
            String jobTitle = job.getTitle() != null ? job.getTitle() : "";
            // Combine title and description for better matching context
            String jobContext = jobTitle + "\n" + jobDescription;

            double matchScore = aiServiceClient.calculateMatchScore(resumeText, jobContext);

            JobResponse jobResponse = JobResponse.builder()
                    .id(job.getId())
                    .title(job.getTitle())
                    .description(job.getDescription())
                    .company(job.getCompany())
                    .location(job.getLocation())
                    .jobType(job.getJobType())
                    .status(job.getStatus())
                    .salaryMin(job.getSalaryMin())
                    .salaryMax(job.getSalaryMax())
                    .requiredSkills(job.getRequiredSkills())
                    .recruiterId(job.getRecruiter().getId())
                    .createdAt(job.getCreatedAt())
                    .updatedAt(job.getUpdatedAt())
                    .build();

            recommendations.add(new JobRecommendationResponse(jobResponse, matchScore));
        }

        // Sort by matchScore descending and return top 10
        List<JobRecommendationResponse> top10 = recommendations.stream()
                .sorted(Comparator.comparingDouble(JobRecommendationResponse::getMatchScore).reversed())
                .limit(10)
                .collect(Collectors.toList());

        log.info("Returning {} job recommendations for user id: {}", top10.size(), userId);
        return top10;
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

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboard(Long userId) {
        log.debug("Getting dashboard stats for user id: {}", userId);

        UserResponse profile = getUserProfile(userId);
        long appCount = applicationRepository.countByApplicantId(userId);
        long savedCount = savedJobRepository.countByUserId(userId);
        long matchedJobs = jobRepository.countByStatusAndDeletedFalse(JobStatus.ACTIVE);

        return DashboardStatsResponse.builder()
                .user(profile)
                .applicationsCount(appCount)
                .savedJobsCount(savedCount)
                .matchedJobs(matchedJobs)
                .build();
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