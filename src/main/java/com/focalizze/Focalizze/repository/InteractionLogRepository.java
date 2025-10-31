package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface InteractionLogRepository extends JpaRepository<InteractionLog,Long> {
    /**
     * Cuenta cuántas interacciones (likes o comentarios) ha realizado un usuario
     * desde un momento específico en el tiempo.
     */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime startOfDay);
}
