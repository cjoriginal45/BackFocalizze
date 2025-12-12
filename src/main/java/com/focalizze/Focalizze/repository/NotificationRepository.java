package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.NotificationClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for managing {@link NotificationClass} entities.
 * Handles fetching, counting, and updating user notifications.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link NotificationClass}.
 * Maneja la obtención, conteo y actualización de notificaciones de usuario.
 */
public interface NotificationRepository extends JpaRepository<NotificationClass,Long> {

    /**
     * Retrieves notifications with eagerly fetched relationships (Trigger User, Thread).
     * <p>
     * Recupera notificaciones con relaciones cargadas ansiosamente (Usuario Desencadenante, Hilo).
     *
     * @param user     The owner of the notifications.
     *                 El propietario de las notificaciones.
     * @param pageable Pagination info.
     *                 Información de paginación.
     * @return A {@link Page} of fully hydrated notifications.
     *         Una {@link Page} de notificaciones completamente hidratadas.
     */
    @Query(value = "SELECT n FROM NotificationClass n " +
            "LEFT JOIN FETCH n.triggerUser " +
            "LEFT JOIN FETCH n.thread " +
            "WHERE n.user = :user",
            countQuery = "SELECT count(n) FROM NotificationClass n WHERE n.user = :user")
    Page<NotificationClass> findByUserWithDetails(@Param("user") User user, Pageable pageable);

    /**
     * Efficiently checks if the user has any unread notifications.
     * <p>
     * Comprueba eficientemente si el usuario tiene notificaciones no leídas.
     *
     * @param user The user to check.
     *             El usuario a verificar.
     * @return {@code true} if unread notifications exist.
     *         {@code true} si existen notificaciones no leídas.
     */
    boolean existsByUserAndIsReadIsFalse(User user);

    /**
     * Marks all notifications as read for a specific user in a single database transaction.
     * Improves performance compared to iterating and saving entities individually.
     * <p>
     * Marca todas las notificaciones como leídas para un usuario específico en una sola transacción de base de datos.
     * Mejora el rendimiento en comparación con iterar y guardar entidades individualmente.
     *
     * @param userId The ID of the user.
     *               El ID del usuario.
     */
    @Modifying
    @Query("UPDATE NotificationClass n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") Long userId);
}
