package com.focalizze.Focalizze.dto;

public record LoginResponseDto(
        Long userId,
        String token,
        String displayName,
        String avatarUrl,
        Integer followingCount,
        Integer followersCount,
        boolean isTwoFactorEnabled
) {

}
