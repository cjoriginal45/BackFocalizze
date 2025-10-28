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

import java.util.List;

@Repository
public interface ThreadRepository extends JpaRepository<ThreadClass,Long> {
    /*
     Obtiene una página de hilos para el feed, trayendo la información del autor
     en la misma consulta para evitar el problema de N+1.
     Gets a page of threads for the feed, fetching author information
     in the same query to avoid the N+1 problem.
     */
    @Query(value = "SELECT t FROM ThreadClass t JOIN FETCH t.user",
            countQuery = "SELECT count(t) FROM ThreadClass t")
    Page<ThreadClass> findThreadsForFeed(Pageable pageable);


    @Query("SELECT t FROM ThreadClass t WHERE EXISTS " +
            "(SELECT 1 FROM t.posts p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ThreadClass> findByPostContentContainingIgnoreCase(@Param("query") String query);

    @Query("SELECT t FROM ThreadClass t WHERE t.category = :category")
    List<ThreadClass> findByCategory(@Param("category") CategoryClass category);

    // Busca una "página" de hilos para un usuario específico, ordenados por fecha de creación descendente.
    Page<ThreadClass> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Contar hilos de un usuario
    long countByUser(User user);
}
