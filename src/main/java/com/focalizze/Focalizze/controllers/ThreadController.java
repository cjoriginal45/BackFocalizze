package com.focalizze.Focalizze.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.ThreadUpdateRequestDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.LikeService;
import com.focalizze.Focalizze.services.SaveService;
import com.focalizze.Focalizze.services.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for managing Threads.
 * Handles creation, updates, deletion, and interactions (likes, saves).
 * <p>
 * Controlador para gestionar Hilos.
 * Maneja creación, actualizaciones, eliminación e interacciones (me gusta, guardados).
 */
@RestController
@RequestMapping("/api/thread")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;
    private final LikeService likeService;
    private final SaveService saveService;

    /**
     * Creates a new thread with optional images.
     * Uses Multipart request to handle JSON and binary data simultaneously.
     * <p>
     * Crea un nuevo hilo con imágenes opcionales.
     * Usa petición Multipart para manejar JSON y datos binarios simultáneamente.
     *
     * @param requestDto The thread data (JSON). / Los datos del hilo (JSON).
     * @param images     List of image files. / Lista de archivos de imagen.
     * @return The created thread DTO. / El DTO del hilo creado.
     */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ThreadResponseDto> createThread(
            @RequestPart("threadRequest") @Valid ThreadRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        ThreadResponseDto response = threadService.createThread(requestDto, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Toggles the 'Like' status on a thread for the authenticated user.
     * <p>
     * Alterna el estado 'Me gusta' en un hilo para el usuario autenticado.
     *
     * @param threadId    The thread ID. / El ID del hilo.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return Empty response. / Respuesta vacía.
     */
    @PostMapping("/{threadId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleLike(
            @PathVariable Long threadId,
            @AuthenticationPrincipal User currentUser
    ) {
        likeService.toggleLike(threadId, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a specific thread by ID.
     * <p>
     * Recupera un hilo específico por ID.
     *
     * @param threadId The thread ID. / El ID del hilo.
     * @return The thread details DTO. / El DTO de detalles del hilo.
     */
    @GetMapping("/{threadId}")
    public ResponseEntity<FeedThreadDto> getThreadById(@PathVariable Long threadId) {
        FeedThreadDto threadDto = threadService.getThreadByIdAndIncrementView(threadId);
        return ResponseEntity.ok(threadDto);
    }

    /**
     * Toggles the 'Saved' status on a thread (Bookmark).
     * <p>
     * Alterna el estado 'Guardado' en un hilo (Marcador).
     *
     * @param threadId    The thread ID. / El ID del hilo.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return Empty response. / Respuesta vacía.
     */
    @PostMapping("/{threadId}/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleSave(
            @PathVariable Long threadId,
            @AuthenticationPrincipal User currentUser
    ) {
        saveService.toggleSave(threadId, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a thread.
     * <p>
     * Elimina un hilo.
     *
     * @param id          The thread ID. / El ID del hilo.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return No content (204). / Sin contenido (204).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteThread(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        threadService.deleteThread(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates an existing thread.
     * <p>
     * Actualiza un hilo existente.
     *
     * @param id          The thread ID. / El ID del hilo.
     * @param updateDto   The update data. / Los datos de actualización.
     * @param currentUser The authenticated user. / El usuario autenticado.
     * @return The updated thread DTO. / El DTO del hilo actualizado.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ThreadResponseDto> updateThread(
            @PathVariable Long id,
            @Valid @RequestBody ThreadUpdateRequestDto updateDto,
            @AuthenticationPrincipal User currentUser
    ) {
        ThreadResponseDto updatedThread = threadService.updateThread(id, updateDto, currentUser);
        return ResponseEntity.ok(updatedThread);
    }

}