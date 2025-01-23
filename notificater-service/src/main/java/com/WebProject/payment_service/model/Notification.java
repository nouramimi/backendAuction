package com.WebProject.payment_service.model;

import com.WebProject.payment_service.dto.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Id
    private String id;
    private String userId;
    private NotificationType type;
    private String message;
    private LocalDateTime timestamp;
    private boolean read;
    private String relatedEntityId;  // auctionId, bidId etc.
    private String relatedEntityType; // "AUCTION", "BID" etc.

}
