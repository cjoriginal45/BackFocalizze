package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.ThreadClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ThreadRepository extends JpaRepository<ThreadClass,Long> {
    /**
     * Obtiene una página de hilos para el feed, trayendo la información del autor
     * en la misma consulta para evitar el problema de N+1.
     *
     * Gets a page of threads for the feed, fetching author information
     * in the same query to avoid the N+1 problem.
     *
     * @param pageable Objeto que contiene la información de paginación y ordenamiento / Object that contains pagination and sorting information.
     * @return Una página de hilos (Page<ThreadClass>) / A page of threads (Page<ThreadClass>)
     */
    @Query(value = "SELECT t FROM ThreadClass t JOIN FETCH t.user",
            countQuery = "SELECT count(t) FROM ThreadClass t")
    Page<ThreadClass> findThreadsForFeed(Pageable pageable);
}
