package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.dto.NotificationTriggerUserDto;
import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Mapper for Notification entities.
 * Includes logic to generate previews of thread content.
 * <p>
 * Mapper para entidades de Notificación.
 * Incluye lógica para generar vistas previas del contenido del hilo.
 */
@Component
public class NotificationMapper {
    private static final int THREAD_PREVIEW_LENGTH = 100;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    /**
     * Converts a Notification entity to DTO.
     * <p>
     * Convierte una entidad Notificación a DTO.
     *
     * @param notification The entity. / La entidad.
     * @return The DTO. / El DTO.
     */
    public NotificationDto toDto(NotificationClass notification) {
        if (notification == null) {
            return null;
        }

        // 1. Extract Trigger User
        User triggerUser = notification.getTriggerUser();
        NotificationTriggerUserDto triggerUserDto = null;
        if (triggerUser != null) {
            triggerUserDto = new NotificationTriggerUserDto(
                    triggerUser.getUsername(),
                    triggerUser.getDisplayName(),
                    triggerUser.getAvatarUrl(defaultAvatarUrl)
            );
        }

        // 2. Create Thread Preview
        String threadPreview = null;
        if (notification.getThread() != null && notification.getThread().getPosts() != null && !notification.getThread().getPosts().isEmpty()) {
            String firstPostContent = notification.getThread().getPosts().stream()
                    .min(Comparator.comparing(Post::getPosition))
                    .map(Post::getContent)
                    .orElse("");

            threadPreview = firstPostContent.length() > THREAD_PREVIEW_LENGTH
                    ? firstPostContent.substring(0, THREAD_PREVIEW_LENGTH)
                    : firstPostContent;
        }

        // 3. Build DTO
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
