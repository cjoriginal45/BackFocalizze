package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Block;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing {@link Block} entities.
 * Handles database operations related to user blocking functionality.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link Block}.
 * Maneja las operaciones de base de datos relacionadas con la funcionalidad de bloqueo de usuarios.
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    /**
     * Finds a specific block relationship between two users.
     * <p>
     * Encuentra una relación de bloqueo específica entre dos usuarios.
     *
     * @param blocker The user who performed the block.
     *                El usuario que realizó el bloqueo.
     * @param blocked The user who was blocked.
     *                El usuario que fue bloqueado.
     * @return An {@link Optional} containing the Block entity if found, otherwise empty.
     *         Un {@link Optional} que contiene la entidad Block si se encuentra, de lo contrario vacío.
     */
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    /**
     * Checks if a block relationship exists between two users.
     * This method is optimized by Spring Data to use a count query internally.
     * <p>
     * Comprueba si existe una relación de bloqueo entre dos usuarios.
     * Este método está optimizado por Spring Data para usar una consulta de conteo internamente.
     *
     * @param blocker The user who performed the block.
     *                El usuario que realizó el bloqueo.
     * @param blocked The user who was blocked.
     *                El usuario que fue bloqueado.
     * @return {@code true} if the block exists, {@code false} otherwise.
     *         {@code true} si el bloqueo existe, {@code false} en caso contrario.
     */
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    /**
     * Retrieves the IDs of users blocked by a specific user.
     * Optimized to return only IDs (projections) instead of full entities.
     * <p>
     * Recupera los IDs de los usuarios bloqueados por un usuario específico.
     * Optimizado para devolver solo IDs (proyecciones) en lugar de entidades completas.
     *
     * @param userId The ID of the user who performed the blocks.
     *               El ID del usuario que realizó los bloqueos.
     * @return A {@link Set} of blocked user IDs.
     *         Un {@link Set} de IDs de usuarios bloqueados.
     */
    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId")
    Set<Long> findBlockedUserIdsByBlocker(@Param("userId") Long userId);

    /**
     * Retrieves the IDs of users who have blocked a specific user.
     * Optimized to return only IDs.
     * <p>
     * Recupera los IDs de los usuarios que han bloqueado a un usuario específico.
     * Optimizado para devolver solo IDs.
     *
     * @param userId The ID of the user who was blocked.
     *               El ID del usuario que fue bloqueado.
     * @return A {@link Set} of blocker user IDs.
     *         Un {@link Set} de IDs de usuarios bloqueadores.
     */
    @Query("SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId")
    Set<Long> findUserIdsWhoBlockedUser(@Param("userId") Long userId);

    /**
     * Filters a given list of author IDs, returning only those that are blocked by the specified user.
     * Useful for filtering feeds or comment sections.
     * <p>
     * Filtra una lista dada de IDs de autores, devolviendo solo aquellos que están bloqueados por el usuario especificado.
     * Útil para filtrar feeds o secciones de comentarios.
     *
     * @param blocker   The user who might have blocked the authors.
     *                  El usuario que podría haber bloqueado a los autores.
     * @param authorIds The set of author IDs to check.
     *                  El conjunto de IDs de autores a verificar.
     * @return A {@link Set} containing only the IDs from {@code authorIds} that are blocked.
     *         Un {@link Set} que contiene solo los IDs de {@code authorIds} que están bloqueados.
     */
    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker = :blocker AND b.blocked.id IN :authorIds")
    Set<Long> findBlockedIdsByBlockerAndBlockedIdsIn(
            @Param("blocker") User blocker,
            @Param("authorIds") Set<Long> authorIds
    );

    /**
     * Retrieves the full User entities blocked by a specific user, ordered by creation date.
     * Use this when user details are required (e.g., "Blocked Users" settings page).
     * <p>
     * Recupera las entidades de Usuario completas bloqueadas por un usuario específico, ordenadas por fecha de creación.
     * Use esto cuando se requieran detalles del usuario (por ejemplo, página de configuración "Usuarios Bloqueados").
     *
     * @param blocker The user who performed the blocks.
     *                El usuario que realizó los bloqueos.
     * @return A {@link List} of blocked {@link User} entities.
     *         Una {@link List} de entidades {@link User} bloqueadas.
     */
    @Query("SELECT b.blocked FROM Block b WHERE b.blocker = :blocker ORDER BY b.createdAt DESC")
    List<User> findBlockedUsersByBlocker(@Param("blocker") User blocker);
}
