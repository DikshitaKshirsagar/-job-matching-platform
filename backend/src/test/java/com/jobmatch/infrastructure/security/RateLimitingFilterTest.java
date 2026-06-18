package com.jobmatch.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
    }

    @Test
    void shouldNotFilter_whenAuthRegisterPath_returnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        assertFalse(rateLimitingFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_whenAuthLoginPath_returnsFalse() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        assertFalse(rateLimitingFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_whenNonAuthPath_returnsTrue() {
        when(request.getRequestURI()).thenReturn("/api/v1/jobs");
        assertTrue(rateLimitingFilter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_whenUnderLimit_allowsRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenRateLimitExceededOnLogin_returns429() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Consume all 5 login tokens
        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // This should exceed the login limit
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Verify that 429 status was set
        verify(response, times(1)).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    void doFilterInternal_whenRateLimitExceededOnRegister_returns429() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        when(request.getRemoteAddr()).thenReturn("192.168.2.1");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Consume all 3 register tokens
        for (int i = 0; i < 3; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // This should exceed the register limit
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Verify that 429 status was set
        verify(response, times(1)).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    void getClientIp_whenXForwardedForPresent_usesThat() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void getClientIp_whenXRealIpPresent_usesThat() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.5");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void differentIps_haveSeparateBuckets() throws Exception {
        // Use first IP to exhaust its login bucket (5 tokens)
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // Second IP should still be allowed
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // First IP should be blocked
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(429);
    }

    @Test
    void loginAndRegisterHaveSeparateBuckets() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        // Exhaust login bucket (5 tokens)
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        for (int i = 0; i < 5; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // Register bucket should still be available (3 tokens) for same IP
        when(request.getRequestURI()).thenReturn("/api/v1/auth/register");
        for (int i = 0; i < 3; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // Login should now be blocked
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(429);
    }
}