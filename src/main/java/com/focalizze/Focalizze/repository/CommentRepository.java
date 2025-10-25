package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CommentClass;
import com.focalizze.Focalizze.models.ThreadClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<CommentClass, Long> {
    // Busca todos los comentarios de un hilo, ordenados por fecha de creación ascendente
    // Find all comments in a thread, sorted by ascending creation date
    Page<CommentClass> findAllByThreadOrderByCreatedAtAsc(ThreadClass thread, Pageable pageable);
}
