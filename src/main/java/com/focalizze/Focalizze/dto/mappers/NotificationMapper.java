package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.dto.NotificationTriggerUserDto;
import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class NotificationMapper {
    private static final int THREAD_PREVIEW_LENGTH = 100;

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;

    public NotificationDto toDto(NotificationClass notification) {
        if (notification == null) {
            return null;
        }

        // --- LÓGICA CORREGIDA ---
        // 1. Obtenemos el 'triggerUser' directamente de la entidad de notificación.
        User triggerUser = notification.getTriggerUser();
        NotificationTriggerUserDto triggerUserDto = null;
        if (triggerUser != null) {
            triggerUserDto = new NotificationTriggerUserDto(
                    triggerUser.getUsername(),
                    triggerUser.getDisplayName(),
                    triggerUser.getAvatarUrl(defaultAvatarUrl)
            );
        }

        // 2. Creamos una vista previa del contenido del hilo (lógica sin cambios, es correcta).
        String threadPreview = null;
        if (notification.getThread() != null && notification.getThread().getPosts() != null && !notification.getThread().getPosts().isEmpty()) {
            // Aseguramos que los posts estén ordenados por posición antes de tomar el primero.
            String firstPostContent = notification.getThread().getPosts().stream()
                    .min(Comparator.comparing(Post::getPosition))
                    .map(Post::getContent)
                    .orElse(""); // Si no hay posts, devuelve una cadena vacía.

            threadPreview = firstPostContent.length() > THREAD_PREVIEW_LENGTH
                    ? firstPostContent.substring(0, THREAD_PREVIEW_LENGTH)
                    : firstPostContent;
        }

        // 3. Construimos el DTO final.
        return new NotificationDto(
                notification.getId(),
                notification.getType().name(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getThread() != null ? notification.getThread().getId() : null,
                threadPreview,
                triggerUserDto
        );
    }
}
