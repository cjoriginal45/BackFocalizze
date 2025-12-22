package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ThreadResponseDto(
        Long id,
        UserDto author,
        String categoryName,
        List<String> posts,
        LocalDateTime createdAt,
        StatsDto stats,
        List<String> images
) {

}
