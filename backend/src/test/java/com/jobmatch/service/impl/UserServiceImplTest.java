package com.jobmatch.service.impl;

import com.jobmatch.api.dto.response.UserResponse;
import com.jobmatch.domain.entity.User;
import com.jobmatch.domain.enums.UserRole;
import com.jobmatch.domain.repository.ApplicationRepository;
import com.jobmatch.domain.repository.JobRepository;
import com.jobmatch.domain.repository.SavedJobRepository;
import com.jobmatch.domain.repository.UserRepository;
import com.jobmatch.exception.FileUploadException;
import com.jobmatch.exception.ResourceNotFoundException;
import com.jobmatch.infrastructure.external.AiServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, applicationRepository, savedJobRepository, jobRepository, aiServiceClient);
        SecurityContextHolder.clearContext();

        ReflectionTestUtils.setField(userService, "maxFileSize", 10485760L);
        ReflectionTestUtils.setField(userService, "maxTextLength", 100000);

        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setRole(UserRole.ROLE_JOB_SEEKER);
    }

    @Test
    void getUserProfile_whenUserExists_returnsProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserProfile(1L);

        assertNotNull(response);
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void getUserProfile_whenUserNotFound_throwsResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserProfile(999L));
    }

    @Test
    void updateUserProfile_whenValidRequest_updatesName() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUserProfile(1L, "Updated Name");

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_whenNullName_throwsIllegalArgument() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfile(1L, null));
    }

    @Test
    void updateUserProfile_whenBlankName_throwsIllegalArgument() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfile(1L, "   "));
    }

    @Test
    void updateUserProfile_whenUserNotFound_throwsResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserProfile(999L, "New Name"));
    }

    @Test
    void uploadResume_whenNullFile_throwsFileUploadException() {
        assertThrows(FileUploadException.class, () -> userService.uploadResume(1L, null));
    }

    @Test
    void uploadResume_whenEmptyFile_throwsFileUploadException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/pdf", new byte[0]);
        assertThrows(FileUploadException.class, () -> userService.uploadResume(1L, emptyFile));
    }

    @Test
    void uploadResume_whenNotPdfExtension_throwsFileUploadException() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "content".getBytes());
        assertThrows(FileUploadException.class, () -> userService.uploadResume(1L, txtFile));
    }

    @Test
    void uploadResume_whenInvalidMimeType_throwsFileUploadException() {
        MockMultipartFile wrongMime = new MockMultipartFile(
                "file", "resume.pdf", "image/png", "%PDF-1.4 content".getBytes());
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThrows(FileUploadException.class, () -> userService.uploadResume(1L, wrongMime));
    }

    @Test
    void uploadResume_whenUserNotFound_throwsResourceNotFound() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "%PDF-1.4 content".getBytes());

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.uploadResume(999L, pdfFile));
    }

    @Test
    void getCurrentUserProfile_whenAuthenticated_returnsProfile() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("john@example.com");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUserProfile();

        assertNotNull(response);
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void getCurrentUserProfile_whenNotAuthenticated_throwsResourceNotFound() {
        SecurityContextHolder.clearContext();

        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUserProfile());
    }
}