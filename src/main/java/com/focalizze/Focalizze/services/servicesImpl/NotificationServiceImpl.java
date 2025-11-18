package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.dto.mappers.NotificationMapper;
import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.NotificationRepository;
import com.focalizze.Focalizze.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;



@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    // SimpMessagingTemplate es la herramienta de Spring para enviar mensajes a través de WebSockets.
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;

    @Override
    public void createAndSendNotification(User userToNotify, NotificationType type, User triggerUser, ThreadClass thread) {
        // CONSTRUCCIÓN DEL MENSAJE: Ahora el mensaje se construye aquí.
        String message = buildMessage(type, triggerUser);

        // 1. Crear y guardar la entidad de notificación.
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

        // 2. Preparar el DTO para enviar al cliente a través de WebSocket.
        NotificationDto dto = notificationMapper.toDto(notification);

        // 3. Enviar el mensaje al "topic" personal del usuario.
        //    Spring se encargará de dirigir este mensaje al cliente correcto.
        //    El destino final será '/user/{username}/queue/notifications'.
        log.info("Enviando notificación a /user/{}/queue/notifications", userToNotify.getUsername());
        messagingTemplate.convertAndSendToUser(
                userToNotify.getUsername(),
                "/queue/notifications",
                dto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsForUser(User user, Pageable pageable) {
        // 1. Buscamos la página de entidades en la base de datos.
        //    (Necesitaremos optimizar esta consulta para evitar N+1).
        Page<NotificationClass> notificationPage = notificationRepository.findByUserWithDetails(user, pageable);

        // 2. Usamos el método 'map' de la página y el mapper para convertir cada entidad a su DTO.
        return notificationPage.map(notificationMapper::toDto);
    }

    /**
     * Método de ayuda para construir el mensaje de la notificación de forma consistente.
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
