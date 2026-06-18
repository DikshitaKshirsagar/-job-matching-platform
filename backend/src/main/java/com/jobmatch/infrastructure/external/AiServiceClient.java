package com.jobmatch.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client for communicating with the external AI matching service.
 * Supports match, batch match, skill gap analysis, collaborative filtering,
 * model info, and drift detection endpoints.
 */
@Slf4j
@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public AiServiceClient(RestTemplate restTemplate,
                           @Value("${app.ai-service.base-url:http://localhost:5000}") String baseUrl,
                           @Value("${app.ai-service.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
    }

    private HttpEntity<Map<String, Object>> buildRequest(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-API-Key", apiKey);
        }
        return new HttpEntity<>(body, headers);
    }

    /**
     * Calls the AI match service to compute a similarity score between a resume and job description.
     */
    public double calculateMatchScore(String resumeText, String jobDescription) {
        if (resumeText == null || resumeText.isBlank() || jobDescription == null || jobDescription.isBlank()) {
            log.warn("Resume text or job description is empty; returning match score 0.0");
            return 0.0;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("resumeText", resumeText);
        request.put("jobDescription", jobDescription);

        try {
            log.debug("Calling AI match service at {}/match", baseUrl);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/match", HttpMethod.POST, buildRequest(request),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("matchScore")) {
                double score = ((Number) response.getBody().get("matchScore")).doubleValue();
                log.info("AI match score computed: {}", score);
                return score;
            }

            log.warn("AI match response did not contain matchScore field");
            return 0.0;
        } catch (ResourceAccessException e) {
            log.error("AI service not reachable at {}: {}. Returning match score 0.0", baseUrl, e.getMessage());
            return 0.0;
        } catch (Exception e) {
            log.error("Error calling AI match service: {}. Returning match score 0.0", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Batch match - compute scores for multiple jobs against a single resume.
     */
    public List<Map<String, Object>> batchMatch(String resumeText, List<Map<String, Object>> jobs) {
        if (resumeText == null || resumeText.isBlank() || jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        Map<String, Object> request = new HashMap<>();
        request.put("resumeText", resumeText);
        request.put("jobs", jobs);

        try {
            log.debug("Calling AI batch match service at {}/match-batch with {} jobs", baseUrl, jobs.size());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/match-batch", HttpMethod.POST, buildRequest(request),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("results")) {
                Object results = response.getBody().get("results");
                if (results instanceof List) {
                    return (List<Map<String, Object>>) results;
                }
            }
        } catch (Exception e) {
            log.error("Error calling AI batch match service: {}", e.getMessage());
        }

        return List.of();
    }

    /**
     * Get skill gap analysis between resume and job description.
     */
    public Map<String, Object> analyzeSkillGap(String resumeText, String jobDescription) {
        Map<String, Object> request = new HashMap<>();
        request.put("resumeText", resumeText);
        request.put("jobDescription", jobDescription);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/skill-gap", HttpMethod.POST, buildRequest(request),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> body = response.getBody();
            // If body is not null and is a Map, return it wrapped in a safe way
            if (body != null) {
                // Create a mutable copy to avoid ClassCastException on Map.of() returned from unchecked cast
                Map<String, Object> result = new HashMap<>(body);
                return result;
            }
        } catch (Exception e) {
            log.error("Error calling AI skill-gap service: {}", e.getMessage());
        }

        return Map.of();
    }

    /**
     * Record user-job interaction for collaborative filtering.
     */
    public boolean recordInteraction(Long userId, Long jobId, String type) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("jobId", jobId);
        request.put("type", type);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/cf-interact", HttpMethod.POST, buildRequest(request),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Failed to record CF interaction: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get collaborative filtering recommendations for a user.
     */
    public List<Map<String, Object>> getCollaborativeRecommendations(Long userId, int topN) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("topN", topN);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/cf-recommend", HttpMethod.POST, buildRequest(request),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() != null && response.getBody().containsKey("recommendations")) {
                Object recommendations = response.getBody().get("recommendations");
                if (recommendations instanceof List) {
                    return (List<Map<String, Object>>) recommendations;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get CF recommendations: {}", e.getMessage());
        }

        return List.of();
    }

    /**
     * Get AI service model info and metrics.
     */
    public Map<String, Object> getModelInfo() {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/model-info", HttpMethod.GET, null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            if (body != null) {
                return new HashMap<>(body);
            }
        } catch (Exception e) {
            log.warn("Failed to get AI model info: {}", e.getMessage());
        }

        return Map.of();
    }

    /**
     * Check for model drift based on recent scores.
     */
    public Map<String, Object> checkDrift(List<Double> recentScores, double threshold) {
        Map<String, Object> request = new HashMap<>();
        request.put("recentScores", recentScores);
        request.put("threshold", threshold);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/drift-check", HttpMethod.POST, buildRequest(request),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            if (body != null) {
                return new HashMap<>(body);
            }
        } catch (Exception e) {
            log.warn("Failed to check model drift: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("driftDetected", false);
        return result;
    }

    /**
     * Health check for the AI service.
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/health", HttpMethod.GET, null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}