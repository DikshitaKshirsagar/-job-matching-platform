package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.JobListResponse;
import com.jobmatch.backend.dto.JobRequest;
import com.jobmatch.backend.dto.JobResponse;
import com.jobmatch.backend.dto.MatchResponse;
import com.jobmatch.backend.entity.Job;
import com.jobmatch.backend.entity.Role;
import com.jobmatch.backend.entity.User;
import com.jobmatch.backend.repository.JobRepository;
import com.jobmatch.backend.repository.UserRepository;
import com.jobmatch.backend.specification.JobSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AiMatchingService aiMatchingService;

    public JobResponse createJob(JobRequest request, String role, Long recruiterId) {
        if (!"RECRUITER".equals(role) || recruiterId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can post jobs");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job payload is required");
        }

        Job job = new Job();
        job.setTitle(request.title().trim());
        job.setCompany(request.company().trim());
        job.setDescription(request.description().trim());
        job.setSkills(request.skills() != null ? request.skills().trim() : null);
        job.setLocation(request.location() != null ? request.location().trim() : null);
        job.setSalary(request.salary() != null ? request.salary().trim() : null);
        job.setRecruiterId(recruiterId);

        return toJobResponse(jobRepository.save(job));
    }

    public Page<JobListResponse> getJobs(String location, String salary, String skills, String query, Pageable pageable) {
        User currentUser = getOptionalCurrentUser();
        var specification = JobSpecification.filter(location, salary, skills, query);
        return jobRepository.findAll(specification, pageable)
                .map(job -> toResponse(job, resolveMatchResponse(currentUser, job), null));
    }

    public JobResponse getJobById(Long id) {
        Long safeId = Objects.requireNonNull(id, "id must not be null");
        return jobRepository.findById(safeId)
                .map(this::toJobResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found with id: " + safeId));
    }

    public Page<JobResponse> getMyJobs(String role, Long recruiterId, Pageable pageable) {
        if (!"RECRUITER".equals(role) || recruiterId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view their posted jobs");
        }

        return jobRepository.findByRecruiterId(recruiterId, pageable)
                .map(this::toJobResponse);
    }

    public List<JobListResponse> getJobRecommendations() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.SEEKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only job seekers can request recommendations");
        }
        if (currentUser.getResumeText() == null || currentUser.getResumeText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload your resume before requesting recommendations");
        }

        return jobRepository.findAll()
                .stream()
                .map(job -> {
                    MatchResponse matchResponse = resolveMatchResponse(currentUser, job);
                    double recommendationScore = calculateRecommendationScore(matchResponse, job, currentUser);
                    return toResponse(job, matchResponse, recommendationScore);
                })
                .sorted(Comparator.comparingDouble((JobListResponse job) -> job.recommendationScore() == null ? 0.0 : job.recommendationScore()).reversed())
                .limit(10)
                .toList();
    }

    private User getOptionalCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private User getCurrentUser() {
        User user = getOptionalCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return user;
    }

    private MatchResponse resolveMatchResponse(User currentUser, Job job) {
        if (currentUser == null || currentUser.getRole() != Role.SEEKER) {
            return new MatchResponse(0.0, List.of(), List.of());
        }
        return aiMatchingService.getMatchResult(currentUser.getResumeText(), job.getDescription());
    }

    private double calculateRecommendationScore(MatchResponse response, Job job, User user) {
        double score = response.getMatchScore();
        score += Math.min(20.0, response.getSkillsMatched().size() * 3.0);
        score -= Math.min(15.0, response.getSkillsMissing().size() * 2.0);
        score += computeExperienceBonus(user.getResumeText(), job.getDescription());
        score += computeTitleCompanyBoost(user.getResumeText(), job);
        return clampScore(score);
    }

    private double computeExperienceBonus(String resumeText, String jobDescription) {
        String resume = resumeText.toLowerCase();
        String job = jobDescription.toLowerCase();
        String[] experienceTerms = {"senior", "lead", "manager", "principal", "junior", "mid"};
        for (String term : experienceTerms) {
            if (resume.contains(term) && job.contains(term)) {
                return 6.0;
            }
        }
        return 0.0;
    }

    private double computeTitleCompanyBoost(String resumeText, Job job) {
        String resume = resumeText.toLowerCase();
        double boost = 0.0;
        if (job.getTitle() != null && resume.contains(job.getTitle().toLowerCase())) {
            boost += 4.0;
        }
        if (job.getCompany() != null && resume.contains(job.getCompany().toLowerCase())) {
            boost += 3.0;
        }
        return boost;
    }

    private double clampScore(double score) {
        return Math.max(0.0, Math.min(100.0, score));
    }

    private JobListResponse toResponse(Job job, MatchResponse matchResponse, Double recommendationScore) {
        return new JobListResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                job.getSkills(),
                job.getLocation(),
                job.getSalary(),
                job.getRecruiterId(),
                job.getCreatedAt(),
                matchResponse != null ? matchResponse.getMatchScore() : null,
                matchResponse != null ? matchResponse.getSkillsMatched() : List.of(),
                matchResponse != null ? matchResponse.getSkillsMissing() : List.of(),
                recommendationScore
        );
    }

    private JobResponse toJobResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                job.getSkills(),
                job.getLocation(),
                job.getSalary(),
                job.getRecruiterId(),
                job.getCreatedAt()
        );
    }
}
