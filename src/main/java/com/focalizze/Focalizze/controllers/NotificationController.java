package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.NotificationDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for managing user notifications.
 * <p>
 * Controlador para gestionar notificaciones de usuario.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves paginated notifications for the current user.
     * <p>
     * Recupera notificaciones paginadas para el usuario actual.
     *
     * @param pageable    Pagination info (Default: sort by createdAt DESC).
     *                    Información de paginación (Por defecto: ordena por createdAt DESC).
     * @return Page of notifications. / Página de notificaciones.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<NotificationDto> notifications = notificationService.getNotificationsForUser(currentUser, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Checks if the user has any unread notifications.
     * <p>
     * Comprueba si el usuario tiene notificaciones no leídas.
     *
     * @return Map with "hasUnread" boolean. / Mapa con booleano "hasUnread".
     */
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> hasUnreadNotifications(@AuthenticationPrincipal User currentUser) {
        boolean hasUnread = notificationService.hasUnreadNotifications(currentUser);
        return ResponseEntity.ok(Map.of("hasUnread", hasUnread));
    }

    /**
     * Marks all notifications as read for the current user.
     * <p>
     * Marca todas las notificaciones como leídas para el usuario actual.
     *
     * @return Empty response. / Respuesta vacía.
     */
    @PostMapping("/mark-as-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok().build();
    }
}
