package com.WebProject.bid_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "bids")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bid {
    @Id
    private String id;
    private String auctionId;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime bidDate;
}
