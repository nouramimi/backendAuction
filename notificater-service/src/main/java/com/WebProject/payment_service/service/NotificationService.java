package com.WebProject.payment_service.service;

import com.WebProject.payment_service.config.WebSocketNotificationService;
import com.WebProject.payment_service.dto.NotificationRequest;
import com.WebProject.payment_service.dto.UserResponse;
import com.WebProject.payment_service.model.Notification;
import com.WebProject.payment_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final WebSocketNotificationService webSocketService;
    private final WebClient.Builder webClientBuilder;

    public Mono<Notification> sendNotification(NotificationRequest request) {
        log.info("Processing notification request: {}", request);
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .message(request.getMessage())
                .timestamp(LocalDateTime.now())
                .read(false)
                .relatedEntityId(request.getRelatedEntityId())
                .relatedEntityType(request.getRelatedEntityType())
                .build();
        return notificationRepository.save(notification)
                .flatMap(saved -> {
                    // Send email for all notification types
                    Mono<Void> emailMono = getUserEmail(saved.getUserId())
                            .flatMap(email -> emailService.sendEmail(
                                    email,
                                    "Auction Notification: " + saved.getType(),
                                    saved.getMessage()
                            ))
                            .onErrorResume(e -> {
                                log.error("Error sending email: {}", e.getMessage());
                                return Mono.empty();
                            });

                    // Send WebSocket notification if needed
                    Mono<Void> webSocketMono = webSocketService.sendNotification(saved)
                            .onErrorResume(e -> {
                                log.error("Error sending WebSocket notification: {}", e.getMessage());
                                return Mono.empty();
                            });

                    return Mono.when(emailMono, webSocketMono)
                            .thenReturn(saved);
                })
                .doOnSuccess(n -> log.info("Notification processed successfully: {}", n))
                .doOnError(e -> log.error("Error processing notification: {}", e.getMessage()));

    }

    private Mono<String> getUserEmail(String userId) {
        log.info("Fetching email for user: {}", userId);
        return webClientBuilder.build()
                .get()
                .uri("http://user-service/api/utilisateurs/" + userId)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .map(UserResponse::getEmail)
                .doOnSuccess(email -> log.info("Retrieved email for user {}: {}", userId, email))
                .doOnError(e -> log.error("Error fetching email for user {}: {}", userId, e.getMessage()));
                //.doOnError(error -> log.error("Error fetching user email: {}", error.getMessage()))
                //.onErrorResume(error -> Mono.empty());
    }

    public Flux<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Flux<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadOrderByTimestampDesc(userId, false);
    }

    public Mono<Notification> markAsRead(String notificationId) {
        return notificationRepository.findById(notificationId)
                .flatMap(notification -> {
                    notification.setRead(true);
                    return notificationRepository.save(notification);
                });
    }
}