package com.WebProject.payment_service.config;

import com.WebProject.payment_service.model.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);
    public Mono<Void> sendNotification(Notification notification) {
        log.info("Sending WebSocket notification: {}", notification);
        return Mono.empty();
    }
}
