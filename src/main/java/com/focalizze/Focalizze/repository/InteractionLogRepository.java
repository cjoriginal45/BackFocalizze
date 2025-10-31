package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
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
}
