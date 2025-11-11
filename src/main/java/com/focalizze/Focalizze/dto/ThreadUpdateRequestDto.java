package com.focalizze.Focalizze.dto;

import jakarta.validation.constraints.Size;

public record ThreadUpdateRequestDto(
        // Hacemos los campos opcionales, pero con validación de tamaño si están presentes
        @Size(max = 280) String post1,
        @Size(max = 140) String post2,
        @Size(max = 70) String post3
) {}
