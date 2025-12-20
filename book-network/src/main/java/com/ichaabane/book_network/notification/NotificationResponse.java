package com.ichaabane.book_network.notification;

import lombok.Builder;

@Builder
public record NotificationResponse(
        Integer id,
        NotificationStatus status,
        String message,
        String bookTitle,
        boolean read,
        String createdAt
) {
    public static NotificationResponse fromEntity(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .message(entity.getMessage())
                .bookTitle(entity.getBookTitle())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt().toString())
                .build();
    }
}
