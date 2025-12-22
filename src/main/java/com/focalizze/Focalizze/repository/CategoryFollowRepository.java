package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.CategoryFollow;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link CategoryFollow} entities.
 * Represents the many-to-many relationship between Users and Categories (Follows).
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link CategoryFollow}.
 * Representa la relación de muchos a muchos entre Usuarios y Categorías (Seguidores).
 */
@Repository
public interface CategoryFollowRepository extends JpaRepository<CategoryFollow,Long> {
    /**
     * Finds a follow relationship between a specific user and a category.
     * <p>
     * Encuentra una relación de seguimiento entre un usuario específico y una categoría.
     *
     * @param user     The user who follows the category.
     *                 El usuario que sigue la categoría.
     * @param category The category being followed.
     *                 La categoría que está siendo seguida.
     * @return An {@link Optional} containing the CategoryFollow entity if it exists.
     *         Un {@link Optional} que contiene la entidad CategoryFollow si existe.
     */
    Optional<CategoryFollow> findByUserAndCategory(User user, CategoryClass category);
}
