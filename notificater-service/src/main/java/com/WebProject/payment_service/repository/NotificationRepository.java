package com.WebProject.payment_service.repository;

import com.WebProject.payment_service.model.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
    Flux<Notification> findByUserIdOrderByTimestampDesc(String userId);
    Flux<Notification> findByUserIdAndReadOrderByTimestampDesc(String userId, boolean read);
}
