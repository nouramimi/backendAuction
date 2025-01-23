package com.WebProject.bid_service.controller;

import com.WebProject.bid_service.dto.BidRequest;
import com.WebProject.bid_service.dto.BidResponse;
import com.WebProject.bid_service.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {
    private final BidService bidService;
    private static final Logger log = LoggerFactory.getLogger(BidController.class);

    @PostMapping
    public Mono<ResponseEntity<BidResponse>> placeBid(@RequestBody BidRequest request) {
        return bidService.placeBid(request)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalStateException.class, e -> {
                    log.error("Invalid auction state: {}", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.error("Invalid bid: {}", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error placing bid: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/auction/{auctionId}/winner")
    public Mono<ResponseEntity<BidResponse>> getWinningBid(@PathVariable String auctionId) {
        return bidService.getWinningBid(auctionId)
                .map(ResponseEntity::ok)
                .onErrorResume(Exception.class, e -> {
                    log.error("Error getting winning bid: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }
}


