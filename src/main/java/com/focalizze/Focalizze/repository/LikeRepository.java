package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    /**
     * Busca una entidad 'Like' específica basada en el usuario y el hilo.
     * Es crucial para la lógica de "toggle" para saber si ya existe un like.
     * @param user El usuario que dio el like.
     * @param thread El hilo que recibió el like.
     * @return Un Optional que contiene el Like si existe, o un Optional vacío si no.
     *
     * * Searches for a specific 'Like' entity based on the user and thread.
     * * This is crucial for the toggle logic to determine if a like already exists.
     * * @param user The user who gave the like.
     * * @param thread The thread that received the like.
     * * @return An Optional containing the Like if it exists, or an empty Optional if not.
     */
    Optional<Like> findByUserAndThread(User user, ThreadClass thread);
}
