package com.focalizze.Focalizze.dto;

public record LoginResponseDto(
        Long userId,
        String token,
        String displayName
) {
}
