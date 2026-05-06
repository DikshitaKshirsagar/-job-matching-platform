package com.jobmatch.backend.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import com.jobmatch.backend.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void validateRequest(HttpServletRequest request, String actionKey) {
        String remoteAddress = request.getRemoteAddr();
        String key = actionKey + ":" + remoteAddress;

        Bucket bucket = buckets.computeIfAbsent(key, this::newBucket);
        if (bucket.tryConsume(1)) {
            return;
        }

        throw new AppException("Too many authentication attempts. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
    }

    private Bucket newBucket(String key) {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(MAX_REQUESTS_PER_MINUTE, Refill.greedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                .build();
    }
}
