package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.InteractionLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for managing user interaction limits.
 * <p>
 * Controlador para gestionar los límites de interacción del usuario.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class InteractionsController {
    private final InteractionLimitService interactionLimitService;

    /**
     * Retrieves the remaining number of interactions allowed for the current user today.
     * <p>
     * Recupera el número restante de interacciones permitidas para el usuario actual hoy.
     *
     * @return Map containing "remaining" and "limit". / Mapa conteniendo "remaining" y "limit".
     */
    @GetMapping("/me/interactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyRemainingInteractions() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int remaining = interactionLimitService.getRemainingInteractions(currentUser);
        return ResponseEntity.ok(Map.of("remaining", remaining, "limit", 20));
    }
}
