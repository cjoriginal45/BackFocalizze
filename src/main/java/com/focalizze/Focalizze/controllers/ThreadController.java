package com.focalizze.Focalizze.controllers;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/thread")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;
    private final LikeService likeService;
    private final SaveService saveService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ThreadResponseDto> createThread(
            // @RequestPart permite recibir el JSON y los Archivos por separado
            @RequestPart("threadRequest") @Valid ThreadRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        ThreadResponseDto response = threadService.createThread(requestDto, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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

    // Método para guardar hilos
    // Method to save threads
    @PostMapping("/{threadId}/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleSave(@PathVariable Long threadId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveService.toggleSave(threadId, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteThread(@PathVariable Long id) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        threadService.deleteThread(id, currentUser);
        // 204 No Content
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ThreadResponseDto> updateThread(
            @PathVariable Long id,
            @Valid @RequestBody ThreadUpdateRequestDto updateDto
    ) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ThreadResponseDto updatedThread = threadService.updateThread(id, updateDto, currentUser);
        return ResponseEntity.ok(updatedThread);
    }

}