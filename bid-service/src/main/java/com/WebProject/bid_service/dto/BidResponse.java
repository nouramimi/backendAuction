package com.WebProject.bid_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BidResponse {
    private String id;
    private String auctionId;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime bidDate;
}
