package com.focalizze.Focalizze.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
