package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link InteractionLog} entities.
 * Tracks user actions for rate limiting, gamification, or audit purposes.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link InteractionLog}.
 * Rastrea las acciones del usuario para limitación de tasas, gamificación o fines de auditoría.
 */
@Repository
public interface InteractionLogRepository extends JpaRepository<InteractionLog,Long> {
    /**
     * Counts the number of specific interactions performed by a user since a given time.
     * <p>
     * Cuenta el número de interacciones específicas realizadas por un usuario desde un momento dado.
     *
     * @param user       The user to check.
     *                   El usuario a verificar.
     * @param startOfDay The timestamp from which to start counting.
     *                   La marca de tiempo desde la cual comenzar a contar.
     * @return The number of interactions found.
     *         El número de interacciones encontradas.
     */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime startOfDay);

    /**
     * Finds the most recent interaction of a specific type for a user since a given time.
     * Returns a simplified Optional (Single Result) compared to fetching a list.
     * <p>
     * Encuentra la interacción más reciente de un tipo específico para un usuario desde un momento dado.
     * Devuelve un Optional simplificado (Resultado Único) en comparación con obtener una lista.
     *
     * @param user       The user who performed the interaction.
     *                   El usuario que realizó la interacción.
     * @param type       The type of interaction (e.g., LIKE, COMMENT).
     *                   El tipo de interacción (ej. LIKE, COMENTARIO).
     * @param startOfDay The timestamp limit.
     *                   El límite de marca de tiempo.
     * @return An {@link Optional} containing the latest interaction log if found.
     *         Un {@link Optional} que contiene el registro de interacción más reciente si se encuentra.
     */
    Optional<InteractionLog> findFirstByUserAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            User user, InteractionType type, LocalDateTime startOfDay);

    /**
     * Retrieves all interaction logs for a user of a specific type within a time range, ordered by most recent.
     * typically used to revert actions (refunds/undo).
     * <p>
     * Recupera todos los registros de interacción para un usuario de un tipo específico dentro de un rango de tiempo, ordenados por el más reciente.
     * Típicamente usado para revertir acciones (reembolsos/deshacer).
     *
     * @param user         The user associated with the logs.
     *                     El usuario asociado con los registros.
     * @param type         The type of interaction to filter.
     *                     El tipo de interacción a filtrar.
     * @param startOfToday The start time filter.
     *                     El filtro de tiempo de inicio.
     * @return A {@link List} of interaction logs.
     *         Una {@link List} de registros de interacción.
     */
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
