package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.models.CategoryClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for managing {@link CategoryClass} entities.
 * Includes optimized DTO projections and direct update queries.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link CategoryClass}.
 * Incluye proyecciones DTO optimizadas y consultas de actualización directa.
 */
@Repository
public interface CategoryRepository extends JpaRepository<CategoryClass,Long> {

    /**
     * Finds a category by its name.
     * <p>
     * Encuentra una categoría por su nombre.
     *
     * @param name The name of the category.
     *             El nombre de la categoría.
     * @return An {@link Optional} containing the category if found.
     *         Un {@link Optional} que contiene la categoría si se encuentra.
     */
    Optional<CategoryClass> findByName(String name);

    /**
     * Atomically increments the followers count for a category.
     * Uses a direct JPQL update to avoid locking and concurrency issues associated with fetch-save.
     * <p>
     * Incrementa atómicamente el conteo de seguidores de una categoría.
     * Utiliza una actualización JPQL directa para evitar problemas de bloqueo y concurrencia asociados con fetch-save.
     *
     * @param categoryId The ID of the category to update.
     *                   El ID de la categoría a actualizar.
     */
    @Modifying
    @Query("UPDATE CategoryClass c SET c.followersCount = c.followersCount + 1 WHERE c.id = :categoryId")
    void incrementFollowersCount(@Param("categoryId") Long categoryId);

    /**
     * Atomically decrements the followers count for a category.
     * Uses a direct JPQL update.
     * <p>
     * Decrementa atómicamente el conteo de seguidores de una categoría.
     * Utiliza una actualización JPQL directa.
     *
     * @param categoryId The ID of the category to update.
     *                   El ID de la categoría a actualizar.
     */
    @Modifying
    @Query("UPDATE CategoryClass c SET c.followersCount = c.followersCount - 1 WHERE c.id = :categoryId")
    void decrementFollowersCount(@Param("categoryId") Long categoryId);

    /**
     * Retrieves detailed information about a category as a DTO by its name.
     * Performs a read-only projection including calculated fields (thread count) and user-specific state (isFollowing).
     * <p>
     * Recupera información detallada sobre una categoría como un DTO por su nombre.
     * Realiza una proyección de solo lectura incluyendo campos calculados (conteo de hilos) y estado específico del usuario (isFollowing).
     *
     * @param name          The name of the category to search for (case-insensitive).
     *                      El nombre de la categoría a buscar (insensible a mayúsculas/minúsculas).
     * @param currentUserId The ID of the currently authenticated user (can be null for guests).
     *                      El ID del usuario actualmente autenticado (puede ser null para invitados).
     * @return An {@link Optional} containing the {@link CategoryDetailsDto} with the projected data.
     *         Un {@link Optional} que contiene el {@link CategoryDetailsDto} con los datos proyectados.
     */
    @Query("SELECT new com.focalizze.Focalizze.dto.CategoryDetailsDto(" +
            "c.id, " +
            "c.name, " +
            "c.description, " +
            "c.imageUrl, " +
            "c.followersCount, " +
            "CAST((SELECT COUNT(t) FROM ThreadClass t WHERE t.category = c AND t.isPublished = true AND t.isDeleted = false) AS Long), " +
            "CASE WHEN (:currentUserId IS NOT NULL) AND EXISTS(" +
            "   SELECT 1 FROM CategoryFollow cf WHERE cf.category = c AND cf.user.id = :currentUserId" +
            ") THEN true ELSE false END" +
            ") " +
            "FROM CategoryClass c " +
            "WHERE lower(c.name) = lower(:name)")
    Optional<CategoryDetailsDto> findCategoryDetailsByName(@Param("name") String name, @Param("currentUserId") Long currentUserId);
}
