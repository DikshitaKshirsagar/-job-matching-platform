package com.jobmatch.api.controller;

import com.jobmatch.domain.entity.Notification;
import com.jobmatch.service.NotificationService;
import com.jobmatch.util.UserIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "WebSocket notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserIdResolver userIdResolver;

    @GetMapping
    @Operation(summary = "Get user notifications", description = "Retrieve paginated notifications for the current user")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userIdResolver.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", notifications.getContent(),
                "totalPages", notifications.getTotalPages(),
                "totalElements", notifications.getTotalElements(),
                "unreadCount", notificationService.getUnreadCount(userId)
        ));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Long userId = userIdResolver.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        Long userId = userIdResolver.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        Long userId = userIdResolver.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "All notifications marked as read"));
    }
}