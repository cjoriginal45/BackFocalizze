package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CommentClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface CommentRepository extends JpaRepository<CommentClass, Long> {

    /**
     * Busca una página de comentarios para un hilo, trayendo la información del autor en la misma consulta.
     * Solo devuelve comentarios que NO están marcados como eliminados.
     * @param thread El hilo del cual obtener los comentarios.
     * @param pageable La información de paginación.
     * @return Una página de comentarios con sus autores.
     */
    @Query(value = "SELECT c FROM CommentClass c JOIN FETCH c.user WHERE c.thread = :thread AND c.isDeleted = false",
            countQuery = "SELECT count(c) FROM CommentClass c WHERE c.thread = :thread AND c.isDeleted = false")
    Page<CommentClass> findActiveCommentsByThread(@Param("thread") ThreadClass thread, Pageable pageable);


    // Método para encontrar un comentario por su ID y su autor (para seguridad).
    Optional<CommentClass> findByIdAndUser(Long id, User user);

    @Query("SELECT c FROM CommentClass c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.replies r " +
            "LEFT JOIN FETCH r.user " +
            "WHERE c.thread = :thread AND c.parent IS NULL AND c.isDeleted = false " +
            "AND c.user.id NOT IN :blockedUserIds " +
            "ORDER BY c.createdAt DESC")
    Page<CommentClass> findActiveRootCommentsByThreadAndFilterBlocked(
            @Param("thread") ThreadClass thread,
            @Param("blockedUserIds") Set<Long> blockedUserIds,
            Pageable pageable
    );
}
