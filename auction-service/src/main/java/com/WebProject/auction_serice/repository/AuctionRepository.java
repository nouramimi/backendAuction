package com.WebProject.auction_serice.repository;

import com.WebProject.auction_serice.dto.AuctionStatus;
import com.WebProject.auction_serice.model.Auction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends MongoRepository<Auction, String> {
    List<Auction> findBySellerId(String sellerId);
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findByStatusAndEndTimeBetween(AuctionStatus status, LocalDateTime start, LocalDateTime end);
}
