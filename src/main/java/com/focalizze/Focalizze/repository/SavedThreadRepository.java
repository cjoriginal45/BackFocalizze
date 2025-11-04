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

@Repository
// El repositorio gestiona la entidad 'SavedThreads'
// The repository manages the 'SavedThreads' entity
public interface SavedThreadRepository extends JpaRepository<SavedThreads, Long> {

    // Para la lógica de "toggle" (añadir/quitar)
    // For "toggle" logic (add/remove)
    Optional<SavedThreads> findByUserAndThread(User user, ThreadClass thread);

    /**
     * Comprueba de forma eficiente si existe una entrada para un usuario y un hilo específicos.
     * Spring Data JPA construye la consulta basándose en el nombre del método, buscando
     * los campos 'user' y 'thread' dentro de la entidad 'SavedThreads'.
     * /
     * Efficiently checks if an entry exists for a specific user and thread.
     * Spring Data JPA constructs the query based on the method name, searching
     * for the 'user' and 'thread' fields within the 'SavedThreads' entity.
     *
     * @param user El usuario que podría haber guardado el hilo / The user who could have saved the thread
     * @param thread El hilo que podría haber sido guardado / The thread that could have been saved
     * @return true si existe una entrada, false en caso contrario / true if an entry exists, false otherwise
     */
    boolean existsByUserAndThread(User user, ThreadClass thread);


    @Query("SELECT s.thread.id FROM SavedThreads s WHERE s.user = :user AND s.thread.id IN :threadIds")
    Set<Long> findSavedThreadIdsByUserInThreadIds(@Param("user") User user, @Param("threadIds") List<Long> threadIds);

    /**
     * Busca todos los hilos guardados por un usuario, devolviendo una página de la entidad 'SavedThreads'.
     * Los resultados se ordenan por la fecha de guardado ('createdAt') de forma descendente.
     * @param user El usuario cuyos hilos guardados se quieren obtener.
     * @param pageable Objeto de paginación y ordenamiento.
     * @return Una página de entidades SavedThreads.
     */
    Page<SavedThreads> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
