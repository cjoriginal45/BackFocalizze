package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.SavedThreads;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing {@link SavedThreads} entities.
 * Enables users to bookmark threads for later viewing.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link SavedThreads}.
 * Permite a los usuarios marcar hilos para verlos más tarde.
 */
@Repository
public interface SavedThreadRepository extends JpaRepository<SavedThreads, Long> {

    /**
     * Finds a specific saved thread entry by user and thread.
     * Used for toggle logic (add/remove bookmark).
     * <p>
     * Encuentra una entrada específica de hilo guardado por usuario e hilo.
     * Usado para la lógica de alternancia (añadir/quitar marcador).
     *
     * @param user   The user who might have saved the thread.
     *               El usuario que podría haber guardado el hilo.
     * @param thread The thread to check.
     *               El hilo a verificar.
     * @return An {@link Optional} containing the entry if found.
     *         Un {@link Optional} que contiene la entrada si se encuentra.
     */
    Optional<SavedThreads> findByUserAndThread(User user, ThreadClass thread);

    /**
     * Efficiently checks if a thread is saved by a user.
     * Uses Spring Data's derived query which internally optimizes to a presence check.
     * <p>
     * Comprueba eficientemente si un hilo está guardado por un usuario.
     * Utiliza la consulta derivada de Spring Data que optimiza internamente a una verificación de presencia.
     *
     * @param user   The user to check.
     *               El usuario a verificar.
     * @param thread The thread to check.
     *               El hilo a verificar.
     * @return {@code true} if the thread is saved, {@code false} otherwise.
     *         {@code true} si el hilo está guardado, {@code false} en caso contrario.
     */
    boolean existsByUserAndThread(User user, ThreadClass thread);

    /**
     * Filters a list of thread IDs, returning only the ones saved by the user.
     * Optimized projection to return a Set for O(1) lookups in the UI layer.
     * <p>
     * Filtra una lista de IDs de hilos, devolviendo solo los guardados por el usuario.
     * Proyección optimizada para devolver un Set para búsquedas O(1) en la capa de UI.
     *
     * @param user      The user who saved the threads.
     *                  El usuario que guardó los hilos.
     * @param threadIds List of thread IDs currently visible in the feed.
     *                  Lista de IDs de hilos actualmente visibles en el feed.
     * @return A {@link Set} of IDs corresponding to saved threads.
     *         Un {@link Set} de IDs correspondientes a hilos guardados.
     */
    @Query("SELECT s.thread.id FROM SavedThreads s WHERE s.user = :user AND s.thread.id IN :threadIds")
    Set<Long> findSavedThreadIdsByUserInThreadIds(@Param("user") User user, @Param("threadIds") List<Long> threadIds);

    /**
     * Retrieves all threads saved by a user, ordered by save date.
     * Includes a {@code JOIN FETCH} to eagerly load the Thread details, preventing N+1 performance issues.
     * <p>
     * Recupera todos los hilos guardados por un usuario, ordenados por fecha de guardado.
     * Incluye un {@code JOIN FETCH} para cargar ansiosamente los detalles del Hilo, evitando problemas de rendimiento N+1.
     *
     * @param user     The user whose saved threads are retrieved.
     *                 El usuario cuyos hilos guardados se recuperan.
     * @param pageable Pagination info.
     *                 Información de paginación.
     * @return A {@link Page} of SavedThreads with the associated ThreadClass initialized.
     *         Una {@link Page} de SavedThreads con la ThreadClass asociada inicializada.
     */
    @Query(value = "SELECT s FROM SavedThreads s JOIN FETCH s.thread WHERE s.user = :user ORDER BY s.createdAt DESC",
            countQuery = "SELECT count(s) FROM SavedThreads s WHERE s.user = :user")
    Page<SavedThreads> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);
}
