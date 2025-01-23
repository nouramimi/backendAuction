package com.WebProject.payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "auction_history")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionHistory {
    @Id
    private String id;
    private String auctionId;
    private String userId;
    private ActionType actionType;
    private LocalDateTime timestamp;
    private String details; // Additional information (e.g., bid amount, final price)

    public enum ActionType {
        CREATED,
        BID,
        WON,
        LOST
    }
}