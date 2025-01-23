package com.WebProject.payment_service.repository;

import com.WebProject.payment_service.model.Payment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface PaymentRepository extends ReactiveMongoRepository<Payment, String> {
    Flux<Payment> findByUserId(String userId);
    Flux<Payment> findByAuctionId(String auctionId);
}