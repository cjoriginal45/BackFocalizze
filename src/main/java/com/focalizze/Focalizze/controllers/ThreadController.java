package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.LikeService;
import com.focalizze.Focalizze.services.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thread")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;
    private final LikeService likeService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ThreadResponseDto> createThread(@Valid @RequestBody ThreadRequestDto threadRequestDto) {
        // Obtenemos el usuario autenticado del contexto de seguridad
        // Get the authenticated user from the security context
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Llamamos al servicio pasándole el DTO y el usuario
        // We call the service passing it the DTO and the user
        ThreadResponseDto response = threadService.createThread(threadRequestDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint para dar o quitar un "like" a un hilo.
     * @param threadId El ID del hilo, extraído de la URL.
     * @return Una respuesta 200 OK si la operación es exitosa.
     *
     * Endpoint for liking or unliking a thread.
     * * @param threadId The thread ID, extracted from the URL.
     * * @return A 200 OK response if the operation is successful.
     */
    @PostMapping("/{threadId}/like")
    @PreAuthorize("isAuthenticated()") // Solo usuarios autenticados pueden dar like / // Only authenticated users can give like
    public ResponseEntity<Void> toggleLike(@PathVariable Long threadId) {
        // Obtenemos el usuario autenticado a partir del token JWT
        // We get the authenticated user from the JWT token
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Llamamos al servicio para que ejecute la lógica
        // We call the service to execute the logic
        likeService.toggleLike(threadId, currentUser);

        // Devolvemos una respuesta exitosa sin contenido
        // We return a successful response without content
        return ResponseEntity.ok().build();
    }

    // Método para obtener un hilo por id
    // Method to get a thread by id
    @GetMapping("/{threadId}")
    public ResponseEntity<FeedThreadDto> getThreadById(@PathVariable Long threadId) {
        FeedThreadDto threadDto = threadService.getThreadByIdAndIncrementView(threadId);
        return ResponseEntity.ok(threadDto);
    }
}