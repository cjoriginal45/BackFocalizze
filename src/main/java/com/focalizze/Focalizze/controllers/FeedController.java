package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.DiscoverFeedService;
import com.focalizze.Focalizze.services.FeedService;
import com.focalizze.Focalizze.services.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing feeds (General, Discover) and feed feedback.
 * <p>
 * Controlador para gestionar feeds (General, Descubrimiento) y retroalimentación del feed.
 */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;
    private final DiscoverFeedService discoverFeedService;
    private final FeedbackService feedbackService;


    /**
     * Retrieves the main feed for the user (Following feed).
     * Uses Spring's PageableDefault to handle sorting automatically.
     * <p>
     * Recupera el feed principal para el usuario (Feed de seguidos).
     *
     * @return Page of threads. / Página de hilos.
     */
    @GetMapping
    public ResponseEntity<Page<FeedThreadDto>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        return ResponseEntity.ok(feedService.getFeed(pageable));
    }


    /**
     * Retrieves the "Discover" feed with recommendations.
     * <p>
     * Recupera el feed "Descubrir" con recomendaciones.
     *
     * @param pageable    Pagination info. / Información de paginación.
     * @return Page of discovery items. / Página de ítems de descubrimiento.
     */
    @GetMapping("/discover")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DiscoverItemDto>> getDiscoverFeed(
            @PageableDefault(size = 20) Pageable pageable) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(discoverFeedService.getDiscoverFeed(currentUser, pageable));
    }

    /**
     * Hides a specific thread from the user's feed.
     * <p>
     * Oculta un hilo específico del feed del usuario.
     *
     * @param threadId    The ID of the thread to hide. / El ID del hilo a ocultar.
     * @param reasonType  The reason for hiding. / La razón para ocultar.
     * @return Empty response. / Respuesta vacía.
     */
    @PostMapping("/feedback/hide")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> hideThread(
            @RequestParam Long threadId,
            @RequestParam String reasonType) {

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        feedbackService.hideThread(threadId, reasonType, currentUser);
        return ResponseEntity.ok().build();
    }
}