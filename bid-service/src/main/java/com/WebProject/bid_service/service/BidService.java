package com.WebProject.bid_service.service;

import com.WebProject.bid_service.dto.AuctionResponse;
import com.WebProject.bid_service.dto.AuctionStatus;
import com.WebProject.bid_service.dto.BidResponse;
import com.WebProject.bid_service.dto.BidRequest;
import com.WebProject.bid_service.model.Bid;
import com.WebProject.bid_service.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {
    private static final Logger log = LoggerFactory.getLogger(BidService.class);
    private final BidRepository bidRepository;
    private final WebClient.Builder webClientBuilder;

    public Mono<BidResponse> placeBid(BidRequest request) {
        return verifyAuction(request.getAuctionId())
                .flatMap(auction -> {
                    validateAuction(auction);
                    return bidRepository.findTopByAuctionIdOrderByAmountDesc(request.getAuctionId())
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());
                })
                .flatMap(highestBidOpt -> {
                    if (highestBidOpt.isPresent() &&
                            request.getAmount().compareTo(highestBidOpt.get().getAmount()) <= 0) {
                        return Mono.error(new IllegalArgumentException(
                                "Bid amount must be higher than current highest bid"));
                    }

                    Bid bid = Bid.builder()
                            .auctionId(request.getAuctionId())
                            .userId(request.getUserId())
                            .amount(request.getAmount())
                            .bidDate(LocalDateTime.now())
                            .build();

                    return bidRepository.save(bid);
                })
                .flatMap(savedBid -> {
                    // Record bid in history
                    Mono<Void> historyMono = webClientBuilder.build().post()
                            .uri("http://history-service/api/history/record")
                            .bodyValue(Map.of(
                                    "auctionId", savedBid.getAuctionId(),
                                    "userId", savedBid.getUserId(),
                                    "actionType", "BID",
                                    "details", "Bid amount: " + savedBid.getAmount()
                            ))
                            .retrieve()
                            .bodyToMono(Void.class);

                    return updateAuctionPrice(request.getAuctionId(), request.getAmount())
                            .then(historyMono)
                            .thenReturn(savedBid);
                })
                .map(this::mapToBidResponse);
    }
    /*public Mono<BidResponse> placeBid(BidRequest request) {
        return verifyAuction(request.getAuctionId())
                .flatMap(auction -> {
                    validateAuction(auction);
                    return bidRepository.findTopByAuctionIdOrderByAmountDesc(request.getAuctionId())
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());
                })
                .flatMap(highestBidOpt -> {
                    if (highestBidOpt.isPresent() &&
                            request.getAmount().compareTo(highestBidOpt.get().getAmount()) <= 0) {
                        return Mono.error(new IllegalArgumentException(
                                "Bid amount must be higher than current highest bid"));
                    }

                    Bid bid = Bid.builder()
                            .auctionId(request.getAuctionId())
                            .userId(request.getUserId())
                            .amount(request.getAmount())
                            .bidDate(LocalDateTime.now())
                            .build();

                    return bidRepository.save(bid);
                })
                .flatMap(savedBid ->
                        updateAuctionPrice(request.getAuctionId(), request.getAmount())
                                .thenReturn(savedBid)
                )
                .map(this::mapToBidResponse)
                .doOnSuccess(response ->
                        log.info("New bid placed for auction {} by user {}",
                                request.getAuctionId(), request.getUserId()));
    }


     */
    private Mono<AuctionResponse> verifyAuction(String auctionId) {
        return webClientBuilder.build()
                .get()
                .uri("http://auction-service/api/auctions/" + auctionId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        error -> Mono.error(new RuntimeException("Auction not found: " + auctionId)))
                .onStatus(status -> status.is5xxServerError(),
                        error -> Mono.error(new RuntimeException("Server error while verifying auction")))
                .bodyToMono(AuctionResponse.class)
                .doOnError(e -> log.error("Error verifying auction {}: {}", auctionId, e.getMessage()));
    }

    private void validateAuction(AuctionResponse auction) {
        if (auction == null) {
            log.error("Auction validation failed: Auction not found");
            throw new RuntimeException("Auction not found");
        }

        log.info("Validating auction: status={}, startTime={}, endTime={}, currentTime={}",
                auction.getStatus(), auction.getStartTime(), auction.getEndTime(), LocalDateTime.now());

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            log.error("Auction validation failed: Auction is not active. Current status: {}", auction.getStatus());
            throw new IllegalStateException("Auction is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime())) {
            log.error("Auction validation failed: Auction has not started yet. Start time: {}, Current time: {}",
                    auction.getStartTime(), now);
            throw new IllegalStateException("Auction has not started yet");
        }

        if (now.isAfter(auction.getEndTime())) {
            log.error("Auction validation failed: Auction has ended. End time: {}, Current time: {}",
                    auction.getEndTime(), now);
            throw new IllegalStateException("Auction has ended");
        }
    }

    private Mono<Void> updateAuctionPrice(String auctionId, BigDecimal newPrice) {
        return webClientBuilder.build()
                .put()
                .uri("http://auction-service/api/auctions/" + auctionId + "/price")
                .bodyValue(newPrice)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("Error updating auction price: {}", e.getMessage()));
    }

    public Mono<BidResponse> getWinningBid(String auctionId) {
        return bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)
                .map(this::mapToBidResponse)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("No bids found for auction: " + auctionId)));
    }

    private BidResponse mapToBidResponse(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuctionId())
                .userId(bid.getUserId())
                .amount(bid.getAmount())
                .bidDate(bid.getBidDate())
                .build();
    }
}