package com.WebProject.payment_service.controller;

import com.WebProject.payment_service.model.AuctionHistory;
import com.WebProject.payment_service.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {
    private final HistoryService historyService;

    @PostMapping("/record")
    public Mono<ResponseEntity<AuctionHistory>> recordAction(
            @RequestParam String auctionId,
            @RequestParam String userId,
            @RequestParam AuctionHistory.ActionType actionType,
            @RequestParam(required = false) String details
    ) {
        return historyService.recordAction(auctionId, userId, actionType, details)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/user/{userId}")
    public Flux<AuctionHistory> getUserHistory(@PathVariable String userId) {
        return historyService.getUserHistory(userId);
    }

    @GetMapping("/auction/{auctionId}")
    public Flux<AuctionHistory> getAuctionHistory(@PathVariable String auctionId) {
        return historyService.getAuctionHistory(auctionId);
    }

    @GetMapping("/user/{userId}/actions/{actionType}")
    public Flux<AuctionHistory> getUserActionHistory(
            @PathVariable String userId,
            @PathVariable AuctionHistory.ActionType actionType
    ) {
        return historyService.getUserActionHistory(userId, actionType);
    }
}