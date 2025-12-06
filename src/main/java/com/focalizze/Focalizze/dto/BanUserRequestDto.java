package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.NotBlank;

public record BanUserRequestDto(
        @NotBlank(message = "El usuario a banear es obligatorio")
        String targetUsername,

        @NotBlank(message = "El motivo es obligatorio")
        String reason,

        @NotBlank(message = "La duración es obligatoria")
        String duration, // "WEEK", "MONTH", "PERMANENT"

        @NotBlank(message = "La contraseña del administrador es obligatoria")
        String adminPassword
) {
}
