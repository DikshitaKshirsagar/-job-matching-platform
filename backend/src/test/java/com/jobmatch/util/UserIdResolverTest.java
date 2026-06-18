package com.jobmatch.util;

import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserIdResolverTest {

    @Mock
    private UserRepository userRepository;

    private UserIdResolver userIdResolver;

    @BeforeEach
    void setUp() {
        userIdResolver = new UserIdResolver(userRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_whenAuthenticatedWithUserPrincipal_returnsUserId() {
        User user = new User();
        user.setId(42L);
        user.setEmail("test@example.com");
        user.setRole(UserRole.ROLE_JOB_SEEKER);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        Long userId = userIdResolver.getCurrentUserId();
        assertEquals(42L, userId);
    }

    @Test
    void getCurrentUserId_whenNoAuthentication_throwsUnauthorized() {
        SecurityContextHolder.clearContext();
        assertThrows(UnauthorizedException.class, () -> userIdResolver.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_whenNotAuthenticated_throwsUnauthorized() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        assertThrows(UnauthorizedException.class, () -> userIdResolver.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_whenPrincipalIsNotUser_throwsUnauthorized() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("not-a-user-object");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        assertThrows(UnauthorizedException.class, () -> userIdResolver.getCurrentUserId());
    }
}