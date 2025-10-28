package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;

public record ProfileResponseDto(
        String username,
        String displayName,
        String avatarUrl,
        String biography,
        Integer followers,
        Integer follow,
        Integer threadCount,
        Integer threadsAvailableToday,
        LocalDateTime registerDate

) {
}
