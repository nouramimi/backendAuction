package com.WebProject.payment_service.service;

import com.WebProject.payment_service.model.AuctionHistory;
import com.WebProject.payment_service.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {
    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);
    private final HistoryRepository historyRepository;

    public Mono<AuctionHistory> recordAction(
            String auctionId,
            String userId,
            AuctionHistory.ActionType actionType,
            String details
    ) {
        AuctionHistory history = AuctionHistory.builder()
                .auctionId(auctionId)
                .userId(userId)
                .actionType(actionType)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();

        return historyRepository.save(history)
                .doOnSuccess(h -> log.info("Recorded action {} for auction {} by user {}",
                        actionType, auctionId, userId))
                .doOnError(e -> log.error("Error recording action: {}", e.getMessage()));
    }

    public Flux<AuctionHistory> getUserHistory(String userId) {
        return historyRepository.findByUserId(userId);
    }

    public Flux<AuctionHistory> getAuctionHistory(String auctionId) {
        return historyRepository.findByAuctionId(auctionId);
    }

    public Flux<AuctionHistory> getUserActionHistory(String userId, AuctionHistory.ActionType actionType) {
        return historyRepository.findByUserIdAndActionType(userId, actionType);
    }
}