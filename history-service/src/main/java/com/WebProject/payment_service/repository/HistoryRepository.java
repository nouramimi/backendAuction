package com.WebProject.payment_service.repository;

import com.WebProject.payment_service.model.AuctionHistory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;


public interface HistoryRepository extends ReactiveMongoRepository<AuctionHistory, String> {
    Flux<AuctionHistory> findByUserId(String userId);
    Flux<AuctionHistory> findByAuctionId(String auctionId);
    Flux<AuctionHistory> findByUserIdAndActionType(String userId, AuctionHistory.ActionType actionType);
}
