package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThreadRequestDto(
        @NotBlank(message = "Post obligatorio")
        @Size(max = 280, message = "El primer post puede tener como maximo 280 caracteres")
        String post1,
        @NotBlank(message = "Post obligatorio")
        @Size(max = 140, message = "El primer post puede tener como maximo 140 caracteres")
        String post2,
        @NotBlank(message = "Post obligatorio")
        @Size(max = 70, message = "El primer post puede tener como maximo 70 caracteres")
        String post3,
        @NotBlank(message = "Categoria obligatoria")
        String category
) {
}
