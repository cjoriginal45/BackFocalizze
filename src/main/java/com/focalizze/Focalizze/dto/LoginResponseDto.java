package com.focalizze.Focalizze.dto;

public record LoginResponseDto(
        Long userId,
        String token,
        String displayName,
        String avatarUrl,
        Integer followingCount,
        Integer followersCount,
        String role,
        boolean isTwoFactorEnabled,
        boolean requiresTwoFactor,
        String message

) {

}
