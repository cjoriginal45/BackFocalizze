package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.CategoryFollowService;
import com.focalizze.Focalizze.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing categories.
 * Handles retrieval of categories and user interactions (following).
 * <p>
 * Controlador para gestionar categorías.
 * Maneja la recuperación de categorías y las interacciones de los usuarios (seguir).
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryFollowService categoryFollowService;

    /**
     * Retrieves all available categories.
     * <p>
     * Recupera todas las categorías disponibles.
     *
     * @return List of categories. / Lista de categorías.
     */
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<CategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Toggles the follow status of a category for the current user.
     * <p>
     * Alterna el estado de seguimiento de una categoría para el usuario actual.
     *
     * @param id          The category ID. / El ID de la categoría.
     * @return Empty response (200 OK). / Respuesta vacía (200 OK).
     */
    @PostMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleFollow(@PathVariable Long id,
                                             @AuthenticationPrincipal User currentUser){
        categoryFollowService.toggleFollowCategory(id, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves details of a specific category by name.
     * <p>
     * Recupera detalles de una categoría específica por nombre.
     *
     * @param name The category name. / El nombre de la categoría.
     * @return Category details DTO. / DTO de detalles de la categoría.
     */
    @GetMapping("/{name}")
    public ResponseEntity<CategoryDetailsDto> getCategoryDetails(@PathVariable String name) {
        return ResponseEntity.ok(categoryService.getCategoryDetails(name));
    }

    /**
     * Retrieves threads belonging to a specific category.
     * <p>
     * Recupera hilos pertenecientes a una categoría específica.
     *
     * @param name     The category name. / El nombre de la categoría.
     * @param pageable Pagination info. / Información de paginación.
     * @return Page of threads. / Página de hilos.
     */
    @GetMapping("/{name}/threads")
    public ResponseEntity<Page<FeedThreadDto>> getThreadsByCategory(
            @PathVariable String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(categoryService.getThreadsByCategory(name, pageable));
    }
}
