package com.jobmatch.backend.security;

import com.jobmatch.backend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // ✅ NOTE: Move this to application.properties in production — never hardcode secrets
    private final Key SECRET = Keys.hmacShaKeyFor(
            "mysecretkeymysecretkeymysecretkey12345".getBytes()
    );

    private final long EXPIRATION = 24 * 60 * 60 * 1000; // 1 day

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                // ✅ FIXED: store role as a String (enum name), not the enum object itself
                // Storing the enum directly can serialize to an object map that fails on extraction
                .claim("role", user.getRole() != null ? user.getRole().name() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return (email != null &&
                    email.equals(userDetails.getUsername()) &&
                    !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            // ✅ Now safely extracted as String since we stored it as .name()
            return extractAllClaims(token).get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Long extractUserId(String token) {
        try {
            Object id = extractAllClaims(token).get("userId");
            return id != null ? Long.valueOf(id.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}