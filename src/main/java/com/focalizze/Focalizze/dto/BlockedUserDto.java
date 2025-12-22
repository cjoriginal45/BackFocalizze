package com.focalizze.Focalizze.dto;

public record BlockedUserDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {
}
