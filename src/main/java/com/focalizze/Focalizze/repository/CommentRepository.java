package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CommentClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing {@link CommentClass} entities.
 * Handles database operations for thread comments, including pagination and filtering.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link CommentClass}.
 * Maneja las operaciones de base de datos para los comentarios de los hilos, incluyendo paginación y filtrado.
 */
@Repository
public interface CommentRepository extends JpaRepository<CommentClass, Long> {

    /**
     * Retrieves a paginated list of active comments for a specific thread, fetching the author eagerly.
     * Uses a specific count query to optimize pagination performance.
     * <p>
     * Recupera una lista paginada de comentarios activos para un hilo específico, cargando el autor ansiosamente.
     * Utiliza una consulta de conteo específica para optimizar el rendimiento de la paginación.
     *
     * @param thread   The thread to retrieve comments for.
     *                 El hilo del cual obtener los comentarios.
     * @param pageable Pagination information (page number, size, sorting).
     *                 Información de paginación (número de página, tamaño, ordenamiento).
     * @return A {@link Page} of comments with their authors initialized.
     *         Una {@link Page} de comentarios con sus autores inicializados.
     */
    @Query(value = "SELECT c FROM CommentClass c JOIN FETCH c.user WHERE c.thread = :thread AND c.isDeleted = false",
            countQuery = "SELECT count(c) FROM CommentClass c WHERE c.thread = :thread AND c.isDeleted = false")
    Page<CommentClass> findActiveCommentsByThread(@Param("thread") ThreadClass thread, Pageable pageable);



    /**
     * Finds a specific comment by its ID and author.
     * Useful for security checks (verifying ownership before editing/deleting).
     * <p>
     * Encuentra un comentario específico por su ID y autor.
     * Útil para verificaciones de seguridad (verificar propiedad antes de editar/eliminar).
     *
     * @param id   The ID of the comment.
     *             El ID del comentario.
     * @param user The user who presumably authored the comment.
     *             El usuario que presumiblemente escribió el comentario.
     * @return An {@link Optional} containing the comment if found and owned by the user.
     *         Un {@link Optional} que contiene el comentario si se encuentra y pertenece al usuario.
     */
    Optional<CommentClass> findByIdAndUser(Long id, User user);

    /**
     * Retrieves root comments (no parent) for a thread, filtering out blocked users.
     * Fetches replies and associated users to populate the comment tree.
     * <p>
     * Recupera comentarios raíz (sin padre) para un hilo, filtrando usuarios bloqueados.
     * Carga las respuestas y los usuarios asociados para poblar el árbol de comentarios.
     *
     * @param thread         The thread to retrieve comments for.
     *                       El hilo del cual obtener los comentarios.
     * @param blockedUserIds A set of User IDs that should be excluded from the results.
     *                       Un conjunto de IDs de usuario que deben ser excluidos de los resultados.
     * @param pageable       Pagination information.
     *                       Información de paginación.
     * @return A {@link Page} of root comments.
     *         Una {@link Page} de comentarios raíz.
     */
    @Query(value = "SELECT c FROM CommentClass c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.replies r " +
            "LEFT JOIN FETCH r.user " +
            "WHERE c.thread = :thread AND c.parent IS NULL AND c.isDeleted = false " +
            "AND c.user.id NOT IN :blockedUserIds " +
            "ORDER BY c.createdAt DESC",
            countQuery = "SELECT count(c) FROM CommentClass c " +
                    "WHERE c.thread = :thread AND c.parent IS NULL AND c.isDeleted = false " +
                    "AND c.user.id NOT IN :blockedUserIds")
    Page<CommentClass> findActiveRootCommentsByThreadAndFilterBlocked(
            @Param("thread") ThreadClass thread,
            @Param("blockedUserIds") Set<Long> blockedUserIds,
            Pageable pageable
    );
}
