package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequestDto(
        @Size(max = 50, message = "El nombre no puede tener más de 50 caracteres")
        String displayName,
        @Size(max = 200, message = "La biografía no puede tener más de 200 caracteres")
        String biography
) {}
