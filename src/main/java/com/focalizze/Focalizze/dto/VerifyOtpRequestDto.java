package com.focalizze.Focalizze.dto;

public record VerifyOtpRequestDto(
        String username,
        String code
) {
}
