package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.HiddenContent;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HiddenContentRepository extends JpaRepository<HiddenContent, Long> {
    // Obtiene los IDs de todos los hilos que el usuario ha ocultado
    @Query("SELECT h.thread.id FROM HiddenContent h WHERE h.user = :user AND h.thread IS NOT NULL")
    List<Long> findHiddenThreadIdsByUser(@Param("user") User user);
}
