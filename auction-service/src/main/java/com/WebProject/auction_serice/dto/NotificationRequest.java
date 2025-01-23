package com.WebProject.auction_serice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private String userId;
    private NotificationType type;
    private String message;
    private String relatedEntityId;
    private String relatedEntityType;
}
