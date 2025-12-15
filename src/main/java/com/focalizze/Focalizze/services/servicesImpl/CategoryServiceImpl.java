package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.CategoryService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link CategoryService} interface.
 * Handles category retrieval, details, and category-specific thread feeds.
 * <p>
 * Implementación de la interfaz {@link CategoryService}.
 * Maneja la recuperación de categorías, detalles y feeds de hilos específicos de categorías.
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;
    private final BlockRepository blockRepository;

    /**
     * Retrieves all available categories.
     * Marks categories as "followed" if the current user follows them.
     * <p>
     * Recupera todas las categorías disponibles.
     * Marca las categorías como "seguidas" si el usuario actual las sigue.
     *
     * @return List of {@link CategoryDto}. / Lista de {@link CategoryDto}.
     */
    @Override
    public List<CategoryDto> getAllCategories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Set<Long> followedCategoryIds = Collections.emptySet();

        // Check if principal is a User instance to determine followed categories
        // Comprobar si el principal es una instancia de User para determinar categorías seguidas
        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {

            // Fetch fresh user data to ensure collections are initialized
            // Obtener datos frescos del usuario para asegurar que las colecciones estén inicializadas
            userRepository.findById(authenticatedUser.getId())
                    .ifPresent(user -> {
                        if (user.getFollowedCategories() != null) {
                            user.getFollowedCategories().forEach(cf ->
                                    followedCategoryIds.add(cf.getCategory().getId()));
                        }
                    });
        }

        List<CategoryClass> allCategories = categoryRepository.findAll();

        final Set<Long> finalFollowedCategoryIds = followedCategoryIds;

        return allCategories.stream().map(category -> new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getFollowersCount(),
                finalFollowedCategoryIds.contains(category.getId())
        )).collect(Collectors.toList());
    }

    /**
     * Retrieves detailed information about a specific category by name.
     * Uses an optimized repository projection.
     * <p>
     * Recupera información detallada sobre una categoría específica por nombre.
     * Utiliza una proyección optimizada del repositorio.
     *
     * @param name The name of the category. / El nombre de la categoría.
     * @return The detailed DTO. / El DTO detallado.
     * @throws EntityNotFoundException If the category is not found. / Si la categoría no se encuentra.
     */
    @Override
    public CategoryDetailsDto getCategoryDetails(String name) {
        Long currentUserId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {
            currentUserId = authenticatedUser.getId();
        }

        return categoryRepository.findCategoryDetailsByName(name, currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found / Categoría no encontrada: " + name));
    }

    /**
     * Retrieves a paginated list of threads belonging to a category.
     * Filters out threads from users blocked by the current user.
     * <p>
     * Recupera una lista paginada de hilos pertenecientes a una categoría.
     * Filtra hilos de usuarios bloqueados por el usuario actual.
     *
     * @param name     The category name. / El nombre de la categoría.
     * @param pageable Pagination info. / Información de paginación.
     * @return A Page of enriched thread DTOs. / Una Página de DTOs de hilos enriquecidos.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getThreadsByCategory(String name, Pageable pageable) {
        // Fetch raw threads from DB (Optimized query)
        // Obtener hilos de la BD (Consulta optimizada)
        Page<ThreadClass> threadPage = threadRepository.findPublishedThreadsByCategoryName(name, pageable);

        User currentUser = null;
        Set<Long> blockedUserIds = new HashSet<>();


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User authenticatedUser) {
            currentUser = authenticatedUser;

            // 1. Fetch blocked IDs (Both directions: I blocked them, or they blocked me)
            // 1. Obtener IDs bloqueados (Ambas direcciones: Yo los bloqueé, o ellos me bloquearon)
            blockedUserIds.addAll(blockRepository.findBlockedUserIdsByBlocker(currentUser.getId()));
            blockedUserIds.addAll(blockRepository.findUserIdsWhoBlockedUser(currentUser.getId()));
        }

        List<ThreadClass> threadsToProcess = threadPage.getContent();

        // 2. Filter in memory if there are blocked users
        // 2. Filtrar en memoria si hay usuarios bloqueados
        if (!blockedUserIds.isEmpty()) {
            threadsToProcess = threadsToProcess.stream()
                    .filter(thread -> !blockedUserIds.contains(thread.getUser().getId()))
                    .toList();
        }

        // 3. Enrich the filtered list (Add like status, save status, etc.)
        // 3. Enriquecer la lista filtrada (Añadir estado de like, estado guardado, etc.)
        List<FeedThreadDto> enrichedDtoList = threadEnricher.enrichList(threadsToProcess, currentUser);

        // 4. Return new Page (Note: Total elements count remains from DB query, effectively hidden items)
        // 4. Devolver nueva Página (Nota: El conteo total de elementos permanece de la consulta BD, ítems efectivamente ocultos)
        return new PageImpl<>(enrichedDtoList, pageable, threadPage.getTotalElements());
    }
}
