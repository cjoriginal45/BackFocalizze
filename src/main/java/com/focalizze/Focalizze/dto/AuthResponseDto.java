package com.focalizze.Focalizze.dto;

import lombok.Builder;

@Builder
public record AuthResponseDto(
        String token,
        boolean requiresTwoFactor,
        String message
) {}