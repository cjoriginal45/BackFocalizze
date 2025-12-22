package com.focalizze.Focalizze.models;

/**
 * Enumeration defining the different types of notifications in the system.
 * <p>
 * Enumeración que define los diferentes tipos de notificaciones en el sistema.
 */
public enum NotificationType {
    /**
     * Notification for a new comment on a thread.
     * Notificación para un nuevo comentario en un hilo.
     */
    NEW_COMMENT,

    /**
     * Notification for a new like on a thread.
     * Notificación para un nuevo "me gusta" en un hilo.
     */
    NEW_LIKE,

    /**
     * Notification when a user starts following the recipient.
     * Notificación cuando un usuario comienza a seguir al destinatario.
     */
    NEW_FOLLOWER,

    /**
     * Notification when a user is explicitly mentioned (@username).
     * Notificación cuando un usuario es mencionado explícitamente (@nombredeusuario).
     */
    MENTION,

    /**
     * Periodic summary of activity.
     * Resumen periódico de actividad.
     */
    DAILY_SUMMARY
}
