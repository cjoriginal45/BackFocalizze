package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FeedThreadDto(
        Long id,
        ThreadResponseDto.UserDto user,
        LocalDateTime publicationDate,
        List<String> posts,
        StatsDto stats,
        boolean isLiked,
        boolean isSaved
) {}
