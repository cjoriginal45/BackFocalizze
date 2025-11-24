package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InteractionLogRepository extends JpaRepository<InteractionLog,Long> {
    /**
     * Cuenta cuántas interacciones (likes o comentarios) ha realizado un usuario
     * desde un momento específico en el tiempo.
     */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime startOfDay);

    // Versión simplificada: encuentra el último LIKE del día.
    Optional<InteractionLog> findFirstByUserAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            User user, InteractionType type, LocalDateTime startOfDay);

    // Buscamos el log más reciente (DESC) de ese usuario, de ese tipo, hecho después del inicio del día.
    @Query("SELECT i FROM InteractionLog i " +
            "WHERE i.user = :user " +
            "AND i.type = :type " +
            "AND i.createdAt >= :startOfToday " +
            "ORDER BY i.createdAt DESC")
    List<InteractionLog> findLogsToRefund(
            @Param("user") User user,
            @Param("type") InteractionType type,
            @Param("startOfToday") LocalDateTime startOfToday);
}
