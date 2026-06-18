package com.jobmatch.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "test-secret-key-that-is-at-least-sixtyfour-bytes-long-for-hs512-signing-algorithm!!");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000L);
    }

    @Test
    void generateTokenFromEmail_whenValidEmail_returnsToken() {
        String token = jwtTokenProvider.generateTokenFromEmail("test@example.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getEmailFromToken_whenValidToken_returnsEmail() {
        String token = jwtTokenProvider.generateTokenFromEmail("test@example.com");
        String email = jwtTokenProvider.getEmailFromToken(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void validateToken_whenValidToken_returnsTrue() {
        String token = jwtTokenProvider.generateTokenFromEmail("test@example.com");

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_whenMalformedToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid-jwt-token"));
    }

    @Test
    void validateToken_whenExpiredToken_returnsFalse() {
        // Create a provider with 0 expiration
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "jwtSecret",
                "test-secret-key-that-is-at-least-sixtyfour-bytes-long-for-hs512-signing-algorithm!!");
        ReflectionTestUtils.setField(expiredProvider, "jwtExpirationMs", -1000L);

        String token = expiredProvider.generateTokenFromEmail("test@example.com");

        // Wait a tiny bit for the negative expiration to take effect
        assertFalse(expiredProvider.validateToken(token));
    }

    @Test
    void validateToken_whenEmptyString_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void getEmailFromToken_whenInvalidToken_throwsException() {
        assertThrows(Exception.class, () -> jwtTokenProvider.getEmailFromToken("invalid-token"));
    }
}