package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequestDto(
        @NotBlank(message = "El comentario no puede estar vacío.")
        @Size(max = 280, message = "El comentario no puede exceder los 280 caracteres.")
        String content
) {
}
