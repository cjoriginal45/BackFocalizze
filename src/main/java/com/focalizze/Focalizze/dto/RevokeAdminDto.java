package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeAdminDto(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String targetUsername,

        @NotBlank(message = "La contraseña es obligatoria")
        String adminPassword
) {
}
