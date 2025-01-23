package com.WebProject.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;
    private String category;
    private String ownerId;
}
