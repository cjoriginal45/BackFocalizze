package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThreadRepository extends JpaRepository<ThreadClass,Long> {
    /*
     Obtiene una página de hilos para el feed, trayendo la información del autor
     en la misma consulta para evitar el problema de N+1.
     Gets a page of threads for the feed, fetching author information
     in the same query to avoid the N+1 problem.
     */

    @Query(value = "SELECT t FROM ThreadClass t " +
            "LEFT JOIN FETCH t.user u " +
            "LEFT JOIN FETCH t.category c " +
            "WHERE t.isPublished = true AND t.isDeleted = false",
            countQuery = "SELECT count(t) FROM ThreadClass t WHERE t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findThreadsForFeed(Pageable pageable);



    /**
     * Obtiene una página de hilos para el feed "Siguiendo" de un usuario.
     * La consulta selecciona hilos que cumplan una de estas condiciones:
     * 1. El autor del hilo (t.user) está en la lista de usuarios que 'currentUser' sigue.
     * 2. La categoría del hilo (t.category) está en la lista de categorías que 'currentUser' sigue.
     *
     * Además, filtra por hilos publicados y no borrados, y ordena por fecha de publicación.
     *
     * @param followedUserIds La lista de IDs de los usuarios que sigue el usuario actual.
     * @param followedCategoryIds La lista de IDs de las categorías que sigue el usuario actual.
     * @param pageable Objeto de paginación.
     * @return Una página de entidades ThreadClass.
     */
    @Query(value = "SELECT t FROM ThreadClass t " +
            "WHERE t.isPublished = true AND t.isDeleted = false " +
            "AND (" +
            "    t.user.id IN :followedUserIds " +
            "    OR t.category.id IN :followedCategoryIds " +
            "    OR t.user.id = :currentUserId" +
            ") " +
            "ORDER BY t.publishedAt DESC",
            countQuery = "SELECT count(t) FROM ThreadClass t " +
                    "WHERE t.isPublished = true AND t.isDeleted = false " +
                    "AND (t.user.id IN :followedUserIds " +
                    "OR t.category.id IN :followedCategoryIds " +
                    "OR t.user.id = :currentUserId)")
    Page<ThreadClass> findFollowingFeed(
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("followedCategoryIds") List<Long> followedCategoryIds,
            @Param("currentUserId") Long currentUserId, // <-- ¡PARÁMETRO NUEVO AÑADIDO!
            Pageable pageable
    );



    @Query("SELECT t FROM ThreadClass t WHERE t.isPublished = true AND t.isDeleted = false AND EXISTS  " +
            "(SELECT 1 FROM t.posts p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ThreadClass> findByPostContentContainingIgnoreCase(@Param("query") String query);

    @Query("SELECT t FROM ThreadClass t WHERE t.category = :category AND t.isPublished = true AND t.isDeleted = false")
    List<ThreadClass> findByCategory(@Param("category") CategoryClass category);

    // Busca una "página" de hilos para un usuario específico, ordenados por fecha de creación descendente.
    Page<ThreadClass> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Contar hilos de un usuario
    long countByUser(User user);


    long countByUserAndCreatedAtAfter(User currentUser, LocalDateTime startOfToday);

    @Query(value = "SELECT t FROM ThreadClass t " +
            "LEFT JOIN FETCH t.user u " +
            "LEFT JOIN FETCH t.category c " +
            "WHERE t.user = :user AND t.isPublished = true AND t.isDeleted = false",
            countQuery = "SELECT count(t) FROM ThreadClass t WHERE t.user = :user AND t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findByUserWithDetails(@Param("user") User user, Pageable pageable);

    @Query("SELECT t FROM ThreadClass t " +
            "LEFT JOIN FETCH t.user u " +
            "LEFT JOIN FETCH t.category c " +
            "LEFT JOIN FETCH t.posts p " +
            "WHERE t.id = :threadId")
    Optional<ThreadClass> findByIdWithDetails(@Param("threadId") Long threadId);

    @Query("SELECT t FROM ThreadClass t WHERE t.isPublished = false AND t.scheduledTime <= :currentTime")
    List<ThreadClass> findThreadsToPublish(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Busca una PÁGINA de hilos que pertenecen a una categoría específica.
     * La consulta también filtra por hilos que están publicados y no eliminados lógicamente.
     * Spring Data JPA generará automáticamente la consulta SQL necesaria, incluyendo la paginación.
     *
     * @param category La entidad de la categoría por la cual filtrar.
     * @param pageable El objeto que contiene la información de paginación (número de página, tamaño).
     * @return Una Page<ThreadClass> con los hilos encontrados y la información de paginación.
     */
    Page<ThreadClass> findByCategoryAndIsPublishedTrueAndIsDeletedFalse(CategoryClass category, Pageable pageable);
}
