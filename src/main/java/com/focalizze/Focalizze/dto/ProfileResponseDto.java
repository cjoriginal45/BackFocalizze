package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;

public record ProfileResponseDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String biography,
        Integer followers,
        Integer follow,
        Integer threadCount,
        Long threadsAvailableToday,
        LocalDateTime registerDate,
        boolean isFollowing

) {
}
