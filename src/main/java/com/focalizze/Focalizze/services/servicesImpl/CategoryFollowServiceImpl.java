package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.CategoryFollow;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryFollowRepository;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.services.CategoryFollowService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of the {@link CategoryFollowService} interface.
 * Manages the logic for users following specific categories.
 * <p>
 * Implementación de la interfaz {@link CategoryFollowService}.
 * Gestiona la lógica para que los usuarios sigan categorías específicas.
 */
@Service
@RequiredArgsConstructor
public class CategoryFollowServiceImpl implements CategoryFollowService {

    private final CategoryRepository categoryRepository;
    private final CategoryFollowRepository categoryFollowRepository;

    /**
     * Toggles the follow status of a category for a user.
     * Updates the category's follower count atomically.
     * <p>
     * Alterna el estado de seguimiento de una categoría para un usuario.
     * Actualiza el conteo de seguidores de la categoría atómicamente.
     *
     * @param categoryId  The ID of the category.
     *                    El ID de la categoría.
     * @param currentUser The user performing the action.
     *                    El usuario que realiza la acción.
     * @throws EntityNotFoundException If the category does not exist.
     *                                 Si la categoría no existe.
     */
    @Override
    @Transactional
    public void toggleFollowCategory(Long categoryId, User currentUser) {
        // Find category or throw exception
        // Buscar categoría o lanzar excepción
        CategoryClass category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found / Categoría no encontrada: " + categoryId));

        // Check if relationship exists
        // Comprobar si existe la relación
        Optional<CategoryFollow> existingFollow = categoryFollowRepository
                .findByUserAndCategory(currentUser, category);

        if (existingFollow.isPresent()) {
            // Unfollow logic
            // Lógica de dejar de seguir
            categoryFollowRepository.delete(existingFollow.get());
            categoryRepository.decrementFollowersCount(categoryId);
        } else {
            // Follow logic
            // Lógica de seguir
            CategoryFollow newFollow = CategoryFollow.builder()
                    .user(currentUser)
                    .category(category)
                    .build();
            categoryFollowRepository.save(newFollow);
            categoryRepository.incrementFollowersCount(categoryId);
        }
    }
}
