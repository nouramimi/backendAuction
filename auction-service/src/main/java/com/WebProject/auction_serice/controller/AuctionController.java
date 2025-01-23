package com.WebProject.auction_serice.controller;

import com.WebProject.auction_serice.dto.AuctionRequest;
import com.WebProject.auction_serice.dto.AuctionResponse;
import com.WebProject.auction_serice.dto.AuctionStatus;
import com.WebProject.auction_serice.model.Auction;
import com.WebProject.auction_serice.repository.AuctionRepository;
import com.WebProject.auction_serice.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor

public class AuctionController {
    private final AuctionService auctionService;

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(@RequestBody AuctionRequest request) {
        return new ResponseEntity<>(auctionService.createAuction(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable String id) {
        return ResponseEntity.ok(auctionService.getAuctionById(id));
    }

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> getAllAuctions() {
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<AuctionResponse> endAuction(@PathVariable String id) {
        return ResponseEntity.ok(auctionService.endAuction(id));
    }
    /*@PutMapping("/{id}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable String id, @RequestBody BigDecimal newPrice) {
        auctionService.updatePrice(id, newPrice);
        return ResponseEntity.ok().build();
    }*/
    @PutMapping("/{id}/price")
    public ResponseEntity<Void> updatePrice(
            @PathVariable String id,
            @RequestParam String bidderId,
            @RequestBody BigDecimal newPrice
    ) {
        auctionService.updatePrice(id, newPrice, bidderId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<AuctionResponse> activateAuction(@PathVariable String id) {
        return ResponseEntity.ok(auctionService.activateAuction(id));
    }

}

