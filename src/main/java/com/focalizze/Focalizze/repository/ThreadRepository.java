package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ThreadRepository extends JpaRepository<ThreadClass,Long> {
    // --- CONSULTAS BASE ---

    @Query("SELECT t FROM ThreadClass t " +
            "LEFT JOIN FETCH t.user u " +
            "LEFT JOIN FETCH t.category c " +
            "LEFT JOIN FETCH t.posts p " +
            "WHERE t.id = :threadId")
    Optional<ThreadClass> findByIdWithDetails(@Param("threadId") Long threadId);

    // --- FEEDS ---

    /**
     * Feed "Siguiendo": Hilos de usuarios seguidos, categorías seguidas O EL PROPIO USUARIO.
     * Incluye filtrado de bloqueados.
     */
    @Query(value = "SELECT t FROM ThreadClass t " +
            "WHERE t.isPublished = true AND t.isDeleted = false " +
            "AND (" +
            "    t.user.id IN :followedUserIds " +
            "    OR t.category.id IN :followedCategoryIds " +
            "    OR t.user.id = :currentUserId" +
            ") " +
            "AND t.user.id NOT IN :blockedUserIds " +
            "ORDER BY t.publishedAt DESC",
            countQuery = "SELECT count(t) FROM ThreadClass t " +
                    "WHERE t.isPublished = true AND t.isDeleted = false " +
                    "AND (t.user.id IN :followedUserIds " +
                    "OR t.category.id IN :followedCategoryIds " +
                    "OR t.user.id = :currentUserId)" +
                    "AND t.user.id NOT IN :blockedUserIds")
    Page<ThreadClass> findFollowingFeed(
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("followedCategoryIds") List<Long> followedCategoryIds,
            @Param("currentUserId") Long currentUserId,
            @Param("blockedUserIds") Set<Long> blockedUserIds,
            Pageable pageable
    );

    /**
     * Feed Global / Invitados
     */
    @Query(value = "SELECT t FROM ThreadClass t " +
            "LEFT JOIN FETCH t.user u " +
            "LEFT JOIN FETCH t.category c " +
            "WHERE t.isPublished = true AND t.isDeleted = false",
            countQuery = "SELECT count(t) FROM ThreadClass t WHERE t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findThreadsForFeed(Pageable pageable);

    // --- PERFIL DE USUARIO ---

    @Query(value = "SELECT t FROM ThreadClass t " +
            "LEFT JOIN FETCH t.user u " +
            "LEFT JOIN FETCH t.category c " +
            "WHERE t.user = :user " +
            "AND t.isPublished = true " +
            "AND t.isDeleted = false " + // 
            "ORDER BY t.publishedAt DESC",
            countQuery = "SELECT count(t) FROM ThreadClass t WHERE t.user = :user AND t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findByUserWithDetails(@Param("user") User user, Pageable pageable);

    // Contar hilos totales activos (para perfil)
    long countByUserAndCreatedAtAfter(User currentUser, LocalDateTime startOfToday);

    // --- RECOMENDACIONES / DESCUBRIR ---

    @Query("SELECT DISTINCT t FROM ThreadClass t " +
            "LEFT JOIN t.likes l " +
            "WHERE t.isPublished = true AND t.isDeleted = false " +
            "AND t.user.id != :currentUserId " +
            "AND t.user.id NOT IN :followedUserIds " +
            "AND t.id NOT IN :hiddenThreadIds " +
            "AND t.user.id NOT IN :blockedUserIds " +
            "AND (" +
            "    t.category.id IN :followedCategoryIds " +
            "    OR l.user.id IN :followedUserIds" +
            ") " +
            "ORDER BY t.publishedAt DESC")
    List<ThreadClass> findRecommendationCandidates(
            @Param("currentUserId") Long currentUserId,
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("followedCategoryIds") List<Long> followedCategoryIds,
            @Param("hiddenThreadIds") Set<Long> hiddenThreadIds,
            @Param("blockedUserIds") Set<Long> blockedUserIds,
            Pageable pageable);

    @Query("SELECT t FROM ThreadClass t " +
            "WHERE t.isPublished = true AND t.isDeleted = false " +
            "AND t.user.id != :currentUserId " +
            "AND t.user.id NOT IN :followedUserIds " +
            "AND t.user.id NOT IN :blockedUserIds " +
            "ORDER BY t.publishedAt DESC")
    Page<ThreadClass> findThreadsForDiscover(
            @Param("currentUserId") Long currentUserId,
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("blockedUserIds") Set<Long> blockedUserIds,
            Pageable pageable
    );

    // --- LÍMITE DIARIO Y VALIDACIONES ---

    // Cuenta hilos creados desde 'start' que NO estén borrados y que estén publicados
    // Esencial para que el límite diario funcione correctamente al borrar
    @Query("SELECT COUNT(t) FROM ThreadClass t " +
            "WHERE t.user = :user " +
            "AND t.createdAt >= :start " +
            "AND t.isPublished = true " +
            "AND t.isDeleted = false")
    long countActiveThreadsSince(@Param("user") User user,
                                 @Param("start") LocalDateTime start);

    // Buscar hilos programados para publicar
    @Query("SELECT t FROM ThreadClass t WHERE t.isPublished = false AND t.scheduledTime <= :currentTime")
    List<ThreadClass> findThreadsToPublish(@Param("currentTime") LocalDateTime currentTime);


    // --- BÚSQUEDA ---

    @Query("SELECT t FROM ThreadClass t WHERE t.isPublished = true AND t.isDeleted = false AND EXISTS  " +
            "(SELECT 1 FROM t.posts p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ThreadClass> findByPostContentContainingIgnoreCase(@Param("query") String query);

    // --- CATEGORÍAS ---

    @Query("SELECT t FROM ThreadClass t " +
            "WHERE lower(t.category.name) = lower(:categoryName) " +
            "AND t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findPublishedThreadsByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    @Query("SELECT t FROM ThreadClass t WHERE t.category = :category AND t.isPublished = true AND t.isDeleted = false")
    List<ThreadClass> findByCategory(@Param("category") CategoryClass category);
}