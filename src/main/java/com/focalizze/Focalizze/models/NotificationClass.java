package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a user notification.
 * Alerts users about interactions related to their content or profile.
 * <p>
 * Entidad que representa una notificación de usuario.
 * Alerta a los usuarios sobre interacciones relacionadas con su contenido o perfil.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * The category/type of the notification.
     * La categoría/tipo de la notificación.
     */
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    /**
     * The textual message of the notification.
     * El mensaje textual de la notificación.
     */
    private String message;

    /**
     * Flag indicating if the user has seen this notification.
     * Bandera que indica si el usuario ha visto esta notificación.
     */
    private boolean isRead;

    /**
     * Timestamp when the notification was generated.
     * Marca de tiempo de cuándo se generó la notificación.
     */
    private LocalDateTime createdAt;

    /**
     * The recipient of the notification.
     * El destinatario de la notificación.
     */
    @ManyToOne
    @JoinColumn(name="user_id")
    @ToString.Exclude
    private User user;

    /**
     * The user who triggered the event (e.g., the liker or commenter).
     * El usuario que desencadenó el evento (ej. quien dio like o comentó).
     */
    @ManyToOne
    @JoinColumn(name = "trigger_user_id")
    @ToString.Exclude
    private User triggerUser;

    /**
     * The thread associated with the event (optional).
     * El hilo asociado con el evento (opcional).
     */
    @ManyToOne
    @JoinColumn(name="thread_id")
    @ToString.Exclude
    private ThreadClass thread;
}
