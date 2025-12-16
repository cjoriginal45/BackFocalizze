package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.dto.mappers.NotificationMapper;
import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.NotificationRepository;
import com.focalizze.Focalizze.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


/**
 * Implementation of the {@link NotificationService} interface.
 * Handles notification creation, persistence, and real-time delivery via WebSockets.
 * <p>
 * Implementación de la interfaz {@link NotificationService}.
 * Maneja la creación, persistencia y entrega en tiempo real de notificaciones vía WebSockets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;
    private final BlockRepository blockRepository;

    /**
     * Creates a notification and sends it asynchronously to the user.
     * Performs a final block check before sending.
     * <p>
     * Crea una notificación y la envía asincrónicamente al usuario.
     * Realiza una verificación final de bloqueo antes de enviar.
     *
     * @param userToNotify The recipient.
     *                     El destinatario.
     * @param type         The notification type.
     *                     El tipo de notificación.
     * @param triggerUser  The user who caused the event (optional).
     *                     El usuario que causó el evento (opcional).
     * @param thread       The associated thread (optional).
     *                     El hilo asociado (opcional).
     */
    @Override
    @Async
    public void createAndSendNotification(User userToNotify, NotificationType type, User triggerUser, ThreadClass thread) {

        if (triggerUser != null) {
            boolean isBlocked = blockRepository.existsByBlockerAndBlocked(userToNotify, triggerUser) ||
                    blockRepository.existsByBlockerAndBlocked(triggerUser, userToNotify);
            if (isBlocked) {
                log.debug("Notification blocked: {} -> {}", triggerUser.getUsername(), userToNotify.getUsername());
                return;
            }
        }

        String message = buildMessage(type, triggerUser);

        // 1. Create and Save Entity / Crear y Guardar Entidad
        NotificationClass notification = NotificationClass.builder()
                .user(userToNotify)
                .triggerUser(triggerUser) // Guardamos quién originó la notificación
                .type(type)
                .message(message) // Guardamos el mensaje generado
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .thread(thread)
                .build();
        notificationRepository.save(notification);

        // 2. Prepare WebSocket DTO / Preparar DTO WebSocket
        NotificationDto dto = notificationMapper.toDto(notification);

        // 3. Send Real-time Update / Enviar Actualización en Tiempo Real
        // Destination: /user/{username}/queue/notifications
        log.debug("Sending WS notification to user: {}", userToNotify.getUsername());
        messagingTemplate.convertAndSendToUser(
                userToNotify.getUsername(),
                "/queue/notifications",
                dto
        );
    }

    /**
     * Retrieves paginated notifications for a user.
     * <p>
     * Recupera notificaciones paginadas para un usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsForUser(User user, Pageable pageable) {
        // Uses optimized FETCH JOIN query from repo
        // Utiliza consulta optimizada FETCH JOIN del repo
        return notificationRepository.findByUserWithDetails(user, pageable)
                .map(notificationMapper::toDto);
    }

    /**
     * Checks for unread notifications efficiently.
     * <p>
     * Comprueba notificaciones no leídas eficientemente.
     */
    @Override
    public boolean hasUnreadNotifications(User user) {
        return notificationRepository.existsByUserAndIsReadIsFalse(user);
    }

    /**
     * Marks all notifications as read in a single batch update.
     * <p>
     * Marca todas las notificaciones como leídas en una sola actualización por lotes.
     */
    @Override
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user.getId());
    }

    /**
     * Helper to build consistent notification messages.
     * <p>
     * Ayuda para construir mensajes de notificación consistentes.
     *
     * @param type The notification type.
     * @return The localized message string.
     */
    private String buildMessage(NotificationType type, User triggerUser) {
        return switch (type) {
            case NEW_LIKE -> " le ha gustado tu hilo.";
            case NEW_COMMENT -> " ha comentado en tu hilo.";
            case NEW_FOLLOWER -> " ha comenzado a seguirte.";
            case MENTION -> " te ha mencionado en un hilo.";
            default -> "Tienes una nueva notificación.";
        };
    }
}
