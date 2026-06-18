package com.jobmatch.util;

import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the current authenticated user's ID from the SecurityContext.
 * Supports two principal types:
 * - {@link User} entity (loaded by CustomUserDetailsService via JWT auth)
 * - String (email) fallback for cases where the principal is just the username
 */
@Component
@RequiredArgsConstructor
public class UserIdResolver {

    private final UserRepository userRepository;

    /**
     * @return the ID of the currently authenticated user
     * @throws UnauthorizedException if no authentication or cannot resolve user ID
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database: " + email));
        }
        throw new UnauthorizedException("Authenticated principal is not a valid User: " + principal.getClass().getName());
    }
}
