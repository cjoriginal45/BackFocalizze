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

/**
 * Repository interface for managing {@link ThreadClass} entities.
 * Handles thread retrieval for feeds, profiles, search, and discovery.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link ThreadClass}.
 * Maneja la recuperación de hilos para feeds, perfiles, búsqueda y descubrimiento.
 */
@Repository
public interface ThreadRepository extends JpaRepository<ThreadClass,Long> {
    // --- CONSULTAS BASE ---

    /**
     * Finds a thread by ID and eagerly fetches its user, category, and posts.
     * Note: Fetching 'posts' collection eagerly might be heavy for large threads.
     * <p>
     * Encuentra un hilo por ID y carga ansiosamente su usuario, categoría y publicaciones.
     * Nota: Cargar la colección 'posts' ansiosamente puede ser pesado para hilos grandes.
     *
     * @param threadId The ID of the thread.
     *                 El ID del hilo.
     * @return An {@link Optional} containing the thread with details.
     *         Un {@link Optional} que contiene el hilo con detalles.
     */
    @Query("""
            SELECT t FROM ThreadClass t
            LEFT JOIN FETCH t.user u
            LEFT JOIN FETCH t.category c
            LEFT JOIN FETCH t.posts p
            WHERE t.id = :threadId
            """)
    Optional<ThreadClass> findByIdWithDetails(@Param("threadId") Long threadId);

    // --- FEEDS ---

    /**
     * "Following" Feed: Threads from followed users, followed categories, OR the user themselves.
     * Filters out blocked users and unpublished content.
     * <p>
     * Feed "Siguiendo": Hilos de usuarios seguidos, categorías seguidas O EL PROPIO USUARIO.
     * Filtra usuarios bloqueados y contenido no publicado.
     *
     * @param followedUserIds     List of IDs of users followed by the current user.
     *                            Lista de IDs de usuarios seguidos por el usuario actual.
     * @param followedCategoryIds List of IDs of categories followed by the current user.
     *                            Lista de IDs de categorías seguidas por el usuario actual.
     * @param currentUserId       The ID of the current user.
     *                            El ID del usuario actual.
     * @param blockedUserIds      Set of IDs of users blocked by or blocking the current user.
     *                            Conjunto de IDs de usuarios bloqueados por o que bloquean al usuario actual.
     * @param pageable            Pagination info.
     *                            Información de paginación.
     * @return A {@link Page} of threads for the feed.
     *         Una {@link Page} de hilos para el feed.
     */
    @Query(value = """
            SELECT t FROM ThreadClass t
            WHERE t.isPublished = true AND t.isDeleted = false
            AND (
                t.user.id IN :followedUserIds
                OR t.category.id IN :followedCategoryIds
                OR t.user.id = :currentUserId
            )
            AND t.user.id NOT IN :blockedUserIds
            ORDER BY t.publishedAt DESC
            """,
            countQuery = """
            SELECT count(t) FROM ThreadClass t
            WHERE t.isPublished = true AND t.isDeleted = false
            AND (
                t.user.id IN :followedUserIds
                OR t.category.id IN :followedCategoryIds
                OR t.user.id = :currentUserId
            )
            AND t.user.id NOT IN :blockedUserIds
            """)
    Page<ThreadClass> findFollowingFeed(
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("followedCategoryIds") List<Long> followedCategoryIds,
            @Param("currentUserId") Long currentUserId,
            @Param("blockedUserIds") Set<Long> blockedUserIds,
            Pageable pageable
    );

    /**
     * Global/Guest Feed: Retrieves all published threads.
     * Uses JOIN FETCH to avoid N+1 problems when displaying author/category.
     * <p>
     * Feed Global/Invitados: Recupera todos los hilos publicados.
     * Utiliza JOIN FETCH para evitar problemas N+1 al mostrar autor/categoría.
     *
     * @param pageable Pagination info.
     *                 Información de paginación.
     * @return A {@link Page} of threads.
     *         Una {@link Page} de hilos.
     */
    @Query(value = """
            SELECT t FROM ThreadClass t
            LEFT JOIN FETCH t.user u
            LEFT JOIN FETCH t.category c
            WHERE t.isPublished = true AND t.isDeleted = false
            """,
            countQuery = "SELECT count(t) FROM ThreadClass t WHERE t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findThreadsForFeed(Pageable pageable);

    /**
     * Retrieves active threads for a specific user profile.
     * <p>
     * Recupera hilos activos para un perfil de usuario específico.
     *
     * @param user     The user whose threads are requested.
     *                 El usuario cuyos hilos se solicitan.
     * @param pageable Pagination info.
     *                 Información de paginación.
     * @return A {@link Page} of the user's threads.
     *         Una {@link Page} de los hilos del usuario.
     */
    @Query(value = """
            SELECT t FROM ThreadClass t
            LEFT JOIN FETCH t.user u
            LEFT JOIN FETCH t.category c
            WHERE t.user = :user
            AND t.isPublished = true
            AND t.isDeleted = false
            ORDER BY t.publishedAt DESC
            """,
            countQuery = "SELECT count(t) FROM ThreadClass t WHERE t.user = :user AND t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findByUserWithDetails(@Param("user") User user, Pageable pageable);

    /**
     * Counts threads created by a user after a certain date.
     * <p>
     * Cuenta los hilos creados por un usuario después de una fecha determinada.
     *
     * @param currentUser  The user.
     *                     El usuario.
     * @param startOfToday The date reference.
     *                     La referencia de fecha.
     * @return The count of threads.
     *         El conteo de hilos.
     */
    long countByUserAndCreatedAtAfter(User currentUser, LocalDateTime startOfToday);

    // --- RECOMENDACIONES / DESCUBRIR ---


    /**
     * Finds candidates for recommendations (Discovery logic).
     * Excludes own threads, already followed users, blocked users, and hidden threads.
     * Prioritizes threads in followed categories or liked by followed users.
     * <p>
     * Encuentra candidatos para recomendaciones (Lógica de descubrimiento).
     * Excluye hilos propios, usuarios ya seguidos, bloqueados e hilos ocultos.
     * Prioriza hilos en categorías seguidas o con "me gusta" de usuarios seguidos.
     *
     * @param currentUserId       Current User ID. / ID Usuario actual.
     * @param followedUserIds     Followed User IDs. / IDs Usuarios seguidos.
     * @param followedCategoryIds Followed Category IDs. / IDs Categorías seguidas.
     * @param hiddenThreadIds     Hidden Thread IDs. / IDs Hilos ocultos.
     * @param blockedUserIds      Blocked User IDs. / IDs Usuarios bloqueados.
     * @param pageable            Pagination info. / Información de paginación.
     * @return List of recommended threads. / Lista de hilos recomendados.
     */
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


    /**
     * General discovery query for threads not followed by the user.
     * <p>
     * Consulta general de descubrimiento para hilos no seguidos por el usuario.
     *
     * @param currentUserId   Current User ID. / ID Usuario actual.
     * @param followedUserIds Followed User IDs. / IDs Usuarios seguidos.
     * @param blockedUserIds  Blocked User IDs. / IDs Usuarios bloqueados.
     * @param pageable        Pagination info. / Información de paginación.
     * @return Page of discovery threads. / Página de hilos de descubrimiento.
     */
    @Query(value = """
            SELECT t FROM ThreadClass t
            WHERE t.isPublished = true AND t.isDeleted = false
            AND t.user.id != :currentUserId
            AND t.user.id NOT IN :followedUserIds
            AND t.user.id NOT IN :blockedUserIds
            ORDER BY t.publishedAt DESC
            """,
            countQuery = """
            SELECT count(t) FROM ThreadClass t
            WHERE t.isPublished = true AND t.isDeleted = false
            AND t.user.id != :currentUserId
            AND t.user.id NOT IN :followedUserIds
            AND t.user.id NOT IN :blockedUserIds
            """)
    Page<ThreadClass> findThreadsForDiscover(
            @Param("currentUserId") Long currentUserId,
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("blockedUserIds") Set<Long> blockedUserIds,
            Pageable pageable
    );

    // --- LÍMITE DIARIO Y VALIDACIONES ---

    /**
     * Counts active (published and not deleted) threads since a specific time.
     * Critical for enforcing daily posting limits.
     * <p>
     * Cuenta hilos activos (publicados y no eliminados) desde un momento específico.
     * Crítico para hacer cumplir los límites de publicación diaria.
     *
     * @param user  The user. / El usuario.
     * @param start Start time. / Tiempo de inicio.
     * @return Count. / Conteo.
     */
    @Query("SELECT COUNT(t) FROM ThreadClass t " +
            "WHERE t.user = :user " +
            "AND t.createdAt >= :start " +
            "AND t.isPublished = true " +
            "AND t.isDeleted = false")
    long countActiveThreadsSince(@Param("user") User user,
                                 @Param("start") LocalDateTime start);

    /**
     * Finds threads scheduled for publication that have reached their trigger time.
     * <p>
     * Encuentra hilos programados para publicación que han alcanzado su tiempo de activación.
     *
     * @param currentTime The current system time.
     *                    La hora actual del sistema.
     * @return List of threads ready to publish.
     *         Lista de hilos listos para publicar.
     */
    @Query("SELECT t FROM ThreadClass t WHERE t.isPublished = false AND t.scheduledTime <= :currentTime")
    List<ThreadClass> findThreadsToPublish(@Param("currentTime") LocalDateTime currentTime);


    // --- BÚSQUEDA ---

    /**
     * Searches for threads containing a specific string in their posts (posts content).
     * Performance Note: Uses LIKE %...% which triggers a full table scan.
     * <p>
     * Busca hilos que contengan una cadena específica en sus publicaciones (contenido de posts).
     * Nota de Rendimiento: Usa LIKE %...% lo que provoca un escaneo completo de la tabla.
     *
     * @param query The search term. / El término de búsqueda.
     * @return List of matching threads. / Lista de hilos coincidentes.
     */
    @Query("SELECT t FROM ThreadClass t WHERE t.isPublished = true AND t.isDeleted = false AND EXISTS  " +
            "(SELECT 1 FROM t.posts p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ThreadClass> findByPostContentContainingIgnoreCase(@Param("query") String query);

    // --- CATEGORÍAS ---

    /**
     * Finds published threads by category name.
     * <p>
     * Encuentra hilos publicados por nombre de categoría.
     *
     * @param categoryName Name of the category. / Nombre de la categoría.
     * @param pageable     Pagination. / Paginación.
     * @return Page of threads. / Página de hilos.
     */
    @Query("SELECT t FROM ThreadClass t " +
            "WHERE lower(t.category.name) = lower(:categoryName) " +
            "AND t.isPublished = true AND t.isDeleted = false")
    Page<ThreadClass> findPublishedThreadsByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    /**
     * Finds all published threads for a category entity.
     * <p>
     * Encuentra todos los hilos publicados para una entidad de categoría.
     *
     * @param category The category entity. / La entidad categoría.
     * @return List of threads. / Lista de hilos.
     */
    @Query("SELECT t FROM ThreadClass t WHERE t.category = :category AND t.isPublished = true AND t.isDeleted = false")
    List<ThreadClass> findByCategory(@Param("category") CategoryClass category);
}