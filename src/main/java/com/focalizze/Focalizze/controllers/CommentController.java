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
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/threads/{threadId}/comments")
    public ResponseEntity<Page<CommentResponseDto>> getComments(
            @PathVariable Long threadId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getCommentsByThread(threadId, pageable));
    }

    @PostMapping("/threads/{threadId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long threadId,
            @Valid @RequestBody CommentRequestDto commentRequestDto) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CommentResponseDto createdComment = commentService.createComment(threadId, commentRequestDto, currentUser);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> editComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto commentRequestDto){
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CommentResponseDto editedComment = commentService.editComment(commentId,commentRequestDto,currentUser);
        return ResponseEntity.ok(editedComment);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDto> replyToComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CommentResponseDto reply = commentService.replyToComment(commentId, request, currentUser);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

}