package com.WebProject.bid_service.repository;

import com.WebProject.bid_service.model.Bid;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends ReactiveMongoRepository<Bid, String> {
    Mono<Bid> findTopByAuctionIdOrderByAmountDesc(String auctionId);
}
