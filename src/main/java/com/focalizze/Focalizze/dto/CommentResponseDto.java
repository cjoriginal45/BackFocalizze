package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;

public record CommentResponseDto(
        Long id,
        String content,
        LocalDateTime createdAt,
        ThreadResponseDto.UserDto author
) {
}
