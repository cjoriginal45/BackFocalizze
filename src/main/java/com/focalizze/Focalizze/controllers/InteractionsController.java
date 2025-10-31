package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.InteractionLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class InteractionsController {

    private final InteractionLimitService interactionLimitService;

    public InteractionsController(InteractionLimitService interactionLimitService) {
        this.interactionLimitService = interactionLimitService;
    }

    // Endpoint para que el usuario autenticado consulte sus interacciones
    @GetMapping("/me/interactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyRemainingInteractions() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int remaining = interactionLimitService.getRemainingInteractions(currentUser);
        return ResponseEntity.ok(Map.of("remaining", remaining, "limit", 20));
    }
}
