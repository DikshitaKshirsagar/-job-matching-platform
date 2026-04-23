package com.jobmatch.backend.security;

import com.jobmatch.backend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole() != null ? user.getRole().name() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
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
                .setSigningKey(getSignKey())
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

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}