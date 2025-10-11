package com.focalizze.Focalizze.dto;

public record RegisterResponse(
        Long userId,
        String username,
        String displayName,
        String email,
        String message
) {
}
