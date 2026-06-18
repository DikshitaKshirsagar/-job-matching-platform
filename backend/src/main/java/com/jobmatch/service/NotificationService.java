package com.jobmatch.service;

import com.jobmatch.domain.entity.Notification;
import com.jobmatch.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createNotification(Long userId, String type, String title, String message,
                                            String relatedEntityType, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);

        Notification saved = notificationRepository.save(notification);

        // Send real-time notification via WebSocket
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    saved
            );
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage());
        }

        log.debug("Notification created for user {}: {}", userId, type);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // Helper methods for specific notification types
    public void notifyNewApplicant(Long recruiterId, Long jobId, String jobTitle, String applicantName) {
        createNotification(
                recruiterId,
                "NEW_APPLICANT",
                "New Application Received",
                applicantName + " applied to " + jobTitle,
                "Job",
                jobId
        );
    }

    public void notifyApplicationStatusUpdate(Long applicantId, Long jobId, String jobTitle, String status) {
        createNotification(
                applicantId,
                "APPLICATION_STATUS",
                "Application Status Updated",
                "Your application for " + jobTitle + " is now " + status,
                "Job",
                jobId
        );
    }

    public void notifyJobRecommendation(Long userId, Long jobId, String jobTitle, String company) {
        createNotification(
                userId,
                "JOB_RECOMMENDATION",
                "Job Recommendation",
                "Check out " + jobTitle + " at " + company,
                "Job",
                jobId
        );
    }

    public void notifyRecruiterAlert(Long recruiterId, String message) {
        createNotification(
                recruiterId,
                "RECRUITER_ALERT",
                "Recruiter Alert",
                message,
                "System",
                null
        );
    }
}