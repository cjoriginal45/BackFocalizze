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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/threads/{threadId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<Page<CommentResponseDto>> getComments(
            @PathVariable Long threadId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getCommentsByThread(threadId, pageable));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long threadId,
            @Valid @RequestBody CommentRequestDto commentRequestDto) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CommentResponseDto createdComment = commentService.createComment(threadId, commentRequestDto, currentUser);
        System.out.println("FECHA DE CREACION: "+createdComment.createdAt());
        System.out.println("CONTENIDO: "+createdComment.content());
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }
}