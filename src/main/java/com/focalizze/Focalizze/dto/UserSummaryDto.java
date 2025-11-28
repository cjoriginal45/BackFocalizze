package com.focalizze.Focalizze.dto;

public record UserSummaryDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        boolean isFollowing // ¿El usuario que hace la petición sigue a este usuario?
) {
}
