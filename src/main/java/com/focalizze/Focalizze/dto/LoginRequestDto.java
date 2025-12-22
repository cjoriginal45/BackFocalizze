package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "El identificador es obligatorio") // identifier is required
        String identifier,
        @NotBlank(message = "La contraseña es obligatoria") // Password is required
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") // Password must be at least 6 characters
        String password
) {
}
