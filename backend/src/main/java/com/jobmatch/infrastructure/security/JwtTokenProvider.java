package com.jobmatch.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret:your-256-bit-secret-key-change-in-production-minimum-32-chars}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromEmail(userDetails.getUsername());
    }

    public String generateTokenFromEmail(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshTokenFromEmail(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getRefreshSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getEmailFromRefreshToken(String token) {
        return parseRefreshClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshClaims(token);
            return true;
        } catch (SecurityException e) {
            log.warn("Invalid refresh JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid refresh JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Refresh JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Refresh JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Refresh JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims parseRefreshClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getRefreshSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("JWT secret must be at least 64 bytes for HS512 signing");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Key getRefreshSigningKey() {
        String refreshSecret = jwtSecret + "_refresh";
        if (refreshSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            // Pad to ensure minimum length
            StringBuilder sb = new StringBuilder(refreshSecret);
            while (sb.toString().getBytes(StandardCharsets.UTF_8).length < 64) {
                sb.append("_pad");
            }
            refreshSecret = sb.toString();
        }
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }
}