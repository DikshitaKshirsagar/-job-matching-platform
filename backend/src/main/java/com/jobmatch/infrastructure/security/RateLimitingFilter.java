package com.jobmatch.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for auth endpoints to prevent brute-force attacks.
 * Separate limits per endpoint:
 * - /auth/login: 5 requests per IP per minute
 * - /auth/register: 3 requests per IP per minute
 * Returns HTTP 429 with Retry-After header on limit exceeded.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // 5 requests per minute for login
    private static final Bandwidth LOGIN_LIMIT = Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build();
    // 3 requests per minute for register
    private static final Bandwidth REGISTER_LIMIT = Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofMinutes(1)).build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only rate-limit auth endpoints
        return !path.matches(".*/auth/(register|login)");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String cacheKey = clientIp + ":" + (path.contains("/login") ? "login" : "register");

        Bucket bucket = cache.computeIfAbsent(cacheKey, this::createBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again after 60 seconds.\"}");
        }
    }

    private Bucket createBucket(String cacheKey) {
        Bandwidth limit;
        if (cacheKey.contains("login")) {
            limit = LOGIN_LIMIT;
        } else {
            limit = REGISTER_LIMIT;
        }
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
