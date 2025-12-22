package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.Size;

public record ThreadUpdateRequestDto(
        // Hacemos los campos opcionales, pero con validación de tamaño si están presentes
        @Size(max = 600) String post1,
        @Size(max = 400) String post2,
        @Size(max = 300) String post3,
        String categoryName
) {}
