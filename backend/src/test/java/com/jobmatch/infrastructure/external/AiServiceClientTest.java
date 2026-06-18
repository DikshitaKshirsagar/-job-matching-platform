package com.jobmatch.infrastructure.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() {
        aiServiceClient = new AiServiceClient(restTemplate, "http://localhost:5000", "");
    }

    @Test
    void calculateMatchScore_whenServiceResponds_returnsScore() {
        Map<String, Object> responseBody = Map.of("matchScore", 85.5);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        double score = aiServiceClient.calculateMatchScore("resume text", "job description");
        assertEquals(85.5, score, 0.001);
    }

    @Test
    void calculateMatchScore_whenResumeTextIsNull_returnsZero() {
        double score = aiServiceClient.calculateMatchScore(null, "job description");
        assertEquals(0.0, score);
    }

    @Test
    void calculateMatchScore_whenJobDescriptionIsBlank_returnsZero() {
        double score = aiServiceClient.calculateMatchScore("resume text", "   ");
        assertEquals(0.0, score);
    }

    @Test
    void calculateMatchScore_whenServiceUnreachable_returnsZero() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        double score = aiServiceClient.calculateMatchScore("resume text", "job description");
        assertEquals(0.0, score);
    }

    @Test
    void calculateMatchScore_whenResponseMissingScore_returnsZero() {
        Map<String, Object> responseBody = Map.of("skillsMatched", java.util.List.of());
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        double score = aiServiceClient.calculateMatchScore("resume text", "job description");
        assertEquals(0.0, score);
    }

    @Test
    void batchMatch_whenServiceResponds_returnsResults() {
        List<Map<String, Object>> mockResults = List.of(Map.of("jobId", 1, "matchScore", 90.0));
        Map<String, Object> responseBody = Map.of("results", mockResults);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        Map<String, Object> job1 = new java.util.HashMap<>();
        job1.put("id", 1);
        job1.put("title", "Test");
        job1.put("description", "desc");
        List<Map<String, Object>> jobs = List.of(job1);
        List<Map<String, Object>> results = aiServiceClient.batchMatch("resume text", jobs);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void analyzeSkillGap_returnsAnalysis() {
        Map<String, Object> responseBody = Map.of(
            "userSkills", List.of("python"),
            "requiredSkills", List.of("java"),
            "skillGap", Map.of("skillsMatched", List.of(), "skillsMissing", List.of("java"), "matchRate", 0.0)
        );
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        Map<String, Object> result = aiServiceClient.analyzeSkillGap("Python", "Java");
        assertFalse(result.isEmpty());
    }

    @Test
    void isHealthy_whenServiceUp_returnsTrue() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "running")));

        assertTrue(aiServiceClient.isHealthy());
    }

    @Test
    void isHealthy_whenServiceDown_returnsFalse() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertFalse(aiServiceClient.isHealthy());
    }

    @Test
    void getModelInfo_returnsInfo() {
        Map<String, Object> mockInfo = Map.of("modelName", "test-model", "totalPredictions", 100);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(mockInfo));

        Map<String, Object> info = aiServiceClient.getModelInfo();
        assertFalse(info.isEmpty());
        assertEquals("test-model", info.get("modelName"));
    }

    @Test
    void checkDrift_returnsResult() {
        Map<String, Object> mockDrift = Map.of("driftDetected", false, "driftScore", 0.05);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(mockDrift));

        Map<String, Object> result = aiServiceClient.checkDrift(List.of(85.0, 90.0), 0.1);
        assertFalse(result.isEmpty());
        assertFalse((Boolean) result.get("driftDetected"));
    }

    @Test
    void recordInteraction_returnsTrue() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "recorded")));

        assertTrue(aiServiceClient.recordInteraction(1L, 2L, "apply"));
    }
}