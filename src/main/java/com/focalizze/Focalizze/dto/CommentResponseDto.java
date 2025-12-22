package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponseDto(
        Long id,
        String content,
        LocalDateTime createdAt,
        UserDto author,
        List<CommentResponseDto> replies
) {
}
