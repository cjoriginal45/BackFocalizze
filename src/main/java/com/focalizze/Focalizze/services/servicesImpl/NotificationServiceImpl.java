package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.NotificationDto;
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

    @Override
    public void createAndSendNotification(User userToNotify, NotificationType type, String message, ThreadClass thread) {
        // 1. Crear y guardar la entidad de notificación en la base de datos.
        NotificationClass notification = NotificationClass.builder()
                .user(userToNotify)
                .type(type)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .thread(thread)
                .build();
        notificationRepository.save(notification);

        // 2. Preparar el DTO para enviar al cliente a través de WebSocket.
        NotificationDto dto = new NotificationDto(notification);

        // 3. Enviar el mensaje al "topic" personal del usuario.
        //    Spring se encargará de dirigir este mensaje al cliente correcto.
        //    El destino final será '/user/{username}/queue/notifications'.
        log.info("Enviando notificación a /user/{}/queue/notifications", userToNotify.getUsername());
        messagingTemplate.convertAndSendToUser(
                userToNotify.getUsername(),
                "/queue/notifications",
                dto
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsForUser(User user, Pageable pageable) {
        // 1. Buscamos la página de entidades en la base de datos.
        Page<NotificationClass> notificationPage = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        // 2. Usamos el método 'map' de la página para convertir cada entidad a su DTO.
        return notificationPage.map(NotificationDto::new); // Usa el constructor del record/DTO
    }
}
