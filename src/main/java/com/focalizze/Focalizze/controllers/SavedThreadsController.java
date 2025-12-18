package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.services.SaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for accessing the current user's saved threads (bookmarks).
 * <p>
 * Controlador para acceder a los hilos guardados (marcadores) del usuario actual.
 */
@RestController
@RequestMapping("/api/saved-threads")
@RequiredArgsConstructor
public class SavedThreadsController {
    private final SaveService saveService;


    /**
     * Retrieves a paginated list of threads saved by the authenticated user.
     * <p>
     * Recupera una lista paginada de hilos guardados por el usuario autenticado.
     *
     * @param pageable Pagination info (default size 10).
     *                 Información de paginación (tamaño por defecto 10).
     * @return Page of saved threads.
     *         Página de hilos guardados.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FeedThreadDto>> getSavedThreads(
            @PageableDefault(size = 10) Pageable pageable){
        Page<FeedThreadDto> savedThreadsPage = saveService.getSavedThreadsForCurrentUser(pageable);
        return ResponseEntity.ok(savedThreadsPage);
    }
}
