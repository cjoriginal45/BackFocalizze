package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Like;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link Like} entities.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link Like}.
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    /**
     * Searches for a specific 'Like' entity based on the user and thread.
     * This is crucial for the toggle logic (add/remove like) to determine if it already exists.
     * <p>
     * Busca una entidad 'Like' específica basada en el usuario y el hilo.
     * Es crucial para la lógica de "toggle" (poner/quitar like) para saber si ya existe.
     *
     * @param user   The user who gave the like.
     *               El usuario que dio el like.
     * @param thread The thread that received the like.
     *               El hilo que recibió el like.
     * @return An {@link Optional} containing the Like if it exists, or empty if not.
     *         Un {@link Optional} que contiene el Like si existe, o vacío si no.
     */
    Optional<Like> findByUserAndThread(User user, ThreadClass thread);
}
