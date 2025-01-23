package com.WebProject.payment_service.controller;

import com.WebProject.payment_service.dto.NotificationRequest;
import com.WebProject.payment_service.model.Notification;
import com.WebProject.payment_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;


    @PostMapping
    public Mono<ResponseEntity<Notification>> sendNotification(@RequestBody NotificationRequest request) {
        return notificationService.sendNotification(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/user/{userId}")
    public Flux<Notification> getUserNotifications(@PathVariable String userId) {
        return notificationService.getUserNotifications(userId);
    }

    @GetMapping("/user/{userId}/unread")
    public Flux<Notification> getUnreadNotifications(@PathVariable String userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @PutMapping("/{id}/read")
    public Mono<ResponseEntity<Notification>> markAsRead(@PathVariable String id) {
        return notificationService.markAsRead(id)
                .map(ResponseEntity::ok);
    }
}
