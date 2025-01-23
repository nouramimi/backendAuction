package com.WebProject.auction_serice.service;


import com.WebProject.auction_serice.dto.*;
import com.WebProject.auction_serice.model.Auction;
import com.WebProject.auction_serice.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {
    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);
    private final AuctionRepository auctionRepository;
    private final WebClient.Builder webClientBuilder;

    public AuctionResponse createAuction(AuctionRequest request) {
        validateAuctionRequest(request);

        Auction auction = Auction.builder()
                .productId(request.getProductId())
                .currentPrice(request.getStartingPrice())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .sellerId(request.getSellerId())
                .status(determineInitialStatus(request.getStartTime()))
                .build();

        Auction savedAuction = auctionRepository.save(auction);
        log.info("Created auction {} for product {}", savedAuction.getId(), request.getProductId());

        //*******
        // Record in history
        webClientBuilder.build().post()
                .uri("http://history-service/api/history/record")
                .bodyValue(Map.of(
                        "auctionId", savedAuction.getId(),
                        "userId", request.getSellerId(),
                        "actionType", "CREATED",
                        "details", "Starting price: " + request.getStartingPrice()
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
        //********
        return mapToAuctionResponse(savedAuction);
    }

    private AuctionStatus determineInitialStatus(LocalDateTime startTime) {
        return LocalDateTime.now().isBefore(startTime) ? AuctionStatus.PENDING : AuctionStatus.ACTIVE;
    }

    private void validateAuctionRequest(AuctionRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (request.getStartingPrice().signum() <= 0) {
            throw new IllegalArgumentException("Starting price must be positive");
        }
    }

    public AuctionResponse getAuctionById(String id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));
        return mapToAuctionResponse(auction);
    }

    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(this::mapToAuctionResponse)
                .collect(Collectors.toList());
    }

    public AuctionResponse endAuction(String id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));

        if (auction.getStatus() == AuctionStatus.ENDED) {
            throw new IllegalStateException("Auction is already ended");
        }

        auction.setStatus(AuctionStatus.ENDED);

        try {
            var winningBid = webClientBuilder.build().get()
                    .uri("http://bid-service/api/bids/auction/" + id + "/winner")
                    .retrieve()
                    .bodyToMono(BidResponse.class)
                    .block();

            if (winningBid != null) {
                auction.setWinningBidderId(winningBid.getUserId());
                auction.setCurrentPrice(winningBid.getAmount());

                // Record winner in history
                webClientBuilder.build().post()
                        .uri("http://history-service/api/history/record")
                        .bodyValue(Map.of(
                                "auctionId", id,
                                "userId", winningBid.getUserId(),
                                "actionType", "WON",
                                "details", "Final price: " + winningBid.getAmount()
                        ))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .subscribe();

                // Record for other participants
                getBiddersForAuction(id).stream()
                        .filter(bidderId -> !bidderId.equals(winningBid.getUserId()))
                        .forEach(bidderId ->
                                webClientBuilder.build().post()
                                        .uri("http://history-service/api/history/record")
                                        .bodyValue(Map.of(
                                                "auctionId", id,
                                                "userId", bidderId,
                                                "actionType", "LOST",
                                                "details", "Winner: " + winningBid.getUserId()
                                        ))
                                        .retrieve()
                                        .bodyToMono(Void.class)
                                        .subscribe()
                        );
            }
        } catch (Exception e) {
            log.error("Error processing auction end: {}", e.getMessage());
        }

        return mapToAuctionResponse(auctionRepository.save(auction));
    }
    /*public AuctionResponse endAuction(String id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));

        if (auction.getStatus() == AuctionStatus.ENDED) {
            throw new IllegalStateException("Auction is already ended");
        }

        auction.setStatus(AuctionStatus.ENDED);

        // Get winning bid information and notify winner
        try {
            var winningBid = webClientBuilder.build().get()
                    .uri("http://bid-service/api/bids/auction/" + id + "/winner")
                    .retrieve()
                    .bodyToMono(BidResponse.class)
                    .block();

            if (winningBid != null) {
                auction.setWinningBidderId(winningBid.getUserId());
                auction.setCurrentPrice(winningBid.getAmount());

                // Notify winner
                notifyAuctionWon(
                        auction.getId(),
                        winningBid.getUserId(),
                        winningBid.getAmount().doubleValue()
                );

            }

            // Notify all participants that the auction has ended
            getBiddersForAuction(auction.getId())
                    .forEach(bidderId ->
                            notifyAuctionEnded(auction.getId(), bidderId)
                    );

        } catch (Exception e) {
            log.error("Error fetching winning bid for auction {}: {}", id, e.getMessage());
        }

        return mapToAuctionResponse(auctionRepository.save(auction));
    }

     */
    private List<String> getBiddersForAuction(String auctionId) {
        try {
            return webClientBuilder.build().get()
                    .uri("http://bid-service/api/bids/auction/" + auctionId + "/bidders")
                    .retrieve()
                    .bodyToFlux(String.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Error fetching bidders for auction {}: {}", auctionId, e.getMessage());
            return List.of();
        }
    }

    @Scheduled(fixedRate = 60000)  // Runs every minute
    public void updateAuctionStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> pendingAuctions = auctionRepository.findByStatus(AuctionStatus.PENDING);

        pendingAuctions.forEach(auction -> {
            if (!now.isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.ACTIVE);
                auctionRepository.save(auction);
                log.info("Activated auction: {}", auction.getId());
            }
        });
    }
    /*public void updatePrice(String id, BigDecimal newPrice) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));
        auction.setCurrentPrice(newPrice);
        auctionRepository.save(auction);
    }*/
    public void updatePrice(String id, BigDecimal newPrice, String newBidderId) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));

        // If there was a previous highest bidder, notify them that they've been outbid
        if (auction.getWinningBidderId() != null && !auction.getWinningBidderId().equals(newBidderId)) {
            notifyOutbid(
                    auction.getId(),
                    auction.getWinningBidderId(),
                    newPrice.doubleValue()
            );
        }

        // Notify about the new bid
        notifyBidPlaced(
                auction.getId(),
                newBidderId,
                newPrice.doubleValue()
        );

        auction.setCurrentPrice(newPrice);
        auction.setWinningBidderId(newBidderId);
        auctionRepository.save(auction);
    }
    @Scheduled(fixedRate = 60000)  // Runs every minute
    public void checkAuctionsEndingSoon() {
        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        List<Auction> endingSoonAuctions = auctionRepository.findByStatusAndEndTimeBetween(
                AuctionStatus.ACTIVE,
                oneHourFromNow.minusMinutes(1),
                oneHourFromNow
        );

        endingSoonAuctions.forEach(auction -> {
            // Notify all bidders for this auction
            getBiddersForAuction(auction.getId())
                    .forEach(bidderId ->
                            notifyAuctionEndingSoon(auction.getId(), bidderId)
                    );
        });
    }
    public AuctionResponse activateAuction(String id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.PENDING) {
            throw new IllegalStateException("Auction is not in PENDING state");
        }

        auction.setStatus(AuctionStatus.ACTIVE);
        auction = auctionRepository.save(auction);
        return mapToAuctionResponse(auction);
    }
    private void notifyAuctionCreated(Auction auction) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(auction.getSellerId())
                .type(NotificationType.EMAIL)
                .message("Your auction for product " + auction.getProductId() + " has been created")
                .relatedEntityId(auction.getId())
                .relatedEntityType("AUCTION")
                .build();

        webClientBuilder.build()
                .post()
                .uri("http://notification-service/api/notifications")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
    }

    private void sendNotification(NotificationRequest request) {
        webClientBuilder.build()
                .post()
                .uri("http://notification-service/api/notifications")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        null,
                        error -> log.error("Failed to send notification: {}", error.getMessage())
                );
    }
    private void notifyBidPlaced(String auctionId, String bidderId, double bidAmount) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(bidderId)
                .type(NotificationType.BID_PLACED)
                .message(String.format("Your bid of $%.2f has been placed on auction %s", bidAmount, auctionId))
                .relatedEntityId(auctionId)
                .relatedEntityType("AUCTION")
                .build();

        sendNotification(request);
    }

    private void notifyOutbid(String auctionId, String outbidUserId, double newBidAmount) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(outbidUserId)
                .type(NotificationType.OUTBID)
                .message(String.format("You've been outbid on auction %s! The new bid is $%.2f", auctionId, newBidAmount))
                .relatedEntityId(auctionId)
                .relatedEntityType("AUCTION")
                .build();

        sendNotification(request);
    }
    private void notifyAuctionEndingSoon(String auctionId, String userId) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.AUCTION_ENDING_SOON)
                .message(String.format("Auction %s is ending in 1 hour! Don't miss out!", auctionId))
                .relatedEntityId(auctionId)
                .relatedEntityType("AUCTION")
                .build();

        sendNotification(request);
    }
    private void notifyAuctionEnded(String auctionId, String userId) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.AUCTION_ENDED)
                .message(String.format("Auction %s has ended.", auctionId))
                .relatedEntityId(auctionId)
                .relatedEntityType("AUCTION")
                .build();

        sendNotification(request);
    }
    private void notifyAuctionWon(String auctionId, String winnerId, double finalPrice) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(winnerId)
                .type(NotificationType.AUCTION_WON)
                .message(String.format("Congratulations! You've won auction %s with a final bid of $%.2f", auctionId, finalPrice))
                .relatedEntityId(auctionId)
                .relatedEntityType("AUCTION")
                .build();

        sendNotification(request);
    }

    private AuctionResponse mapToAuctionResponse(Auction auction) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .productId(auction.getProductId())
                .currentPrice(auction.getCurrentPrice())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .sellerId(auction.getSellerId())
                .winningBidderId(auction.getWinningBidderId())
                .status(auction.getStatus())
                .build();
    }
}


