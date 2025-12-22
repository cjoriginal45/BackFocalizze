package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.HiddenContent;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository interface for managing {@link HiddenContent} entities.
 * Used to store preferences for content that a user wishes to hide.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link HiddenContent}.
 * Se utiliza para almacenar preferencias de contenido que un usuario desea ocultar.
 */
@Repository
public interface HiddenContentRepository extends JpaRepository<HiddenContent, Long> {
    /**
     * Retrieves the IDs of all threads hidden by a specific user.
     * Returns a Set for O(1) lookup performance during feed filtering.
     * <p>
     * Recupera los IDs de todos los hilos ocultos por un usuario específico.
     * Devuelve un Set para un rendimiento de búsqueda O(1) durante el filtrado del feed.
     *
     * @param user The user whose hidden content is retrieved.
     *             El usuario cuyo contenido oculto se recupera.
     * @return A {@link Set} of hidden thread IDs.
     *         Un {@link Set} de IDs de hilos ocultos.
     */
    @Query("SELECT h.thread.id FROM HiddenContent h WHERE h.user = :user AND h.thread IS NOT NULL")
    Set<Long> findHiddenThreadIdsByUser(@Param("user") User user);
}
