package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.CommentRequestDto;
import com.focalizze.Focalizze.dto.CommentResponseDto;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    Page<CommentResponseDto> getCommentsByThread(Long threadId, Pageable pageable);

    CommentResponseDto createComment(Long threadId, CommentRequestDto commentRequestDto, User currentUser);

    void deleteComment(Long commentId, User currentUser);

    CommentResponseDto editComment(Long commentId, CommentRequestDto commentRequestDto, User currentUser);

    CommentResponseDto replyToComment(Long parentCommentId, CommentRequestDto request, User currentUser);
}
