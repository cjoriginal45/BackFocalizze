package com.focalizze.Focalizze.dto;

import com.focalizze.Focalizze.models.NotificationClass;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String type,
        String message,
        boolean isRead,
        LocalDateTime createdAt,
        Long threadId
// Puedes añadir más datos aquí, como el avatar del usuario que generó la notificación
) {
    public NotificationDto(NotificationClass n) {
        this(n.getId(), n.getType().name(), n.getMessage(), n.isRead(), n.getCreatedAt(), (n.getThread() != null) ? n.getThread().getId() : null);
    }
}
