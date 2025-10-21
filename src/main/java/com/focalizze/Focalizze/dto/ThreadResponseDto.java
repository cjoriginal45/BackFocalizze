package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ThreadResponseDto(
        Long id,
        UserDto author,
        String categoryName,
        List<String> posts,
        LocalDateTime createdAt
) {
    // Sub-record para anidar la información del autor de forma segura
    // Sub-record to safely nest author information
    public record UserDto(
            Long id,
            String username,
            String displayName
    ) {
    }
}
