package com.focalizze.Focalizze.dto;

import com.focalizze.Focalizze.models.NotificationClass;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String type,
        String message,
        boolean isRead,
        LocalDateTime createdAt,
        Long threadId, // ID del hilo relacionado
        String threadPreview, // Un extracto del primer post del hilo
        NotificationTriggerUserDto triggerUser // El usuario que causó la notificación
// Puedes añadir más datos aquí, como el avatar del usuario que generó la notificación
) {

}
