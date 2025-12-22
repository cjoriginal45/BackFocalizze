package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.CommentRequestDto;
import com.focalizze.Focalizze.dto.CommentResponseDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing comments.
 * Handles CRUD operations for comments on threads.
 * <p>
 * Controlador para gestionar comentarios.
 * Maneja operaciones CRUD para comentarios en hilos.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    /**
     * Retrieves a paginated list of comments for a specific thread.
     * <p>
     * Recupera una lista paginada de comentarios para un hilo específico.
     *
     * @param threadId The thread ID. / El ID del hilo.
     * @param pageable Pagination info. / Información de paginación.
     * @return Page of comments. / Página de comentarios.
     */
    @GetMapping("/threads/{threadId}/comments")
    public ResponseEntity<Page<CommentResponseDto>> getComments(
            @PathVariable Long threadId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getCommentsByThread(threadId, pageable));
    }

    /**
     * Creates a new comment on a thread.
     * <p>
     * Crea un nuevo comentario en un hilo.
     *
     * @param threadId    The thread ID. / El ID del hilo.
     * @param commentRequestDto     The comment data. / Los datos del comentario.
     * @return The created comment. / El comentario creado.
     */
    @PostMapping("/threads/{threadId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long threadId,
            @Valid @RequestBody CommentRequestDto commentRequestDto,
            @AuthenticationPrincipal User currentUser) {
        CommentResponseDto createdComment = commentService.createComment(threadId, commentRequestDto, currentUser);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    /**
     * Edits an existing comment.
     * <p>
     * Edita un comentario existente.
     *
     * @param commentId   The comment ID. / El ID del comentario.
     * @param commentRequestDto     The new comment data. / Los nuevos datos del comentario.
     * @return The updated comment. / El comentario actualizado.
     */
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> editComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto commentRequestDto,
            @AuthenticationPrincipal User currentUser){
        CommentResponseDto editedComment = commentService.editComment(commentId,commentRequestDto,currentUser);
        return ResponseEntity.ok(editedComment);
    }

    /**
     * Deletes a comment.
     * <p>
     * Elimina un comentario.
     *
     * @param commentId   The comment ID. / El ID del comentario.
     * @return No content (204). / Sin contenido (204).
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                              @AuthenticationPrincipal User currentUser) {
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Replies to an existing comment (Nested comment).
     * <p>
     * Responde a un comentario existente (Comentario anidado).
     *
     * @param commentId   The parent comment ID. / El ID del comentario padre.
     * @param request     The reply data. / Los datos de la respuesta.
     * @return The created reply. / La respuesta creada.
     */
    @PostMapping("/comments/{commentId}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> replyToComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        CommentResponseDto reply = commentService.replyToComment(commentId, request, currentUser);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

}