package com.jobmatch.backend.service;

import com.jobmatch.backend.dto.MatchRequest;
import com.jobmatch.backend.dto.MatchResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMatchingService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    public MatchResponse getMatchResult(String resumeText, String jobDescription) {
        if (resumeText == null || resumeText.isBlank() || jobDescription == null || jobDescription.isBlank()) {
            return new MatchResponse(0.0, java.util.List.of(), java.util.List.of());
        }

        try {
            MatchRequest request = new MatchRequest();
            request.setResumeText(resumeText);
            request.setJobDescription(jobDescription);

            MatchResponse response = restTemplate.postForObject(
                    aiServiceUrl + "/match",
                    request,
                    MatchResponse.class
            );

            if (response == null) {
                return new MatchResponse(0.0, java.util.List.of(), java.util.List.of());
            }

            double clampedScore = Math.max(0.0, Math.min(100.0, response.getMatchScore()));
            response.setMatchScore(clampedScore);
            response.setSkillsMatched(response.getSkillsMatched() != null ? response.getSkillsMatched() : java.util.List.of());
            response.setSkillsMissing(response.getSkillsMissing() != null ? response.getSkillsMissing() : java.util.List.of());
            return response;
        } catch (Exception ex) {
            log.error("AI matching failed: {}", ex.getMessage());
            return new MatchResponse(0.0, java.util.List.of(), java.util.List.of());
        }
    }

    public double getMatchScore(String resumeText, String jobDescription) {
        return getMatchResult(resumeText, jobDescription).getMatchScore();
    }
}
