package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thread")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService; // Asumiendo que tienes una interfaz y la implementación se llama ThreadServiceImpl

    @PostMapping("/create")
    // Si usas seguridad a nivel de método, asegúrate de que sea correcta
    // Por ejemplo, para permitir a cualquier usuario logueado:
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ThreadResponseDto> createThread(@Valid @RequestBody ThreadRequestDto threadRequestDto) {
        // Obtenemos el usuario autenticado del contexto de seguridad
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Llamamos al servicio pasándole el DTO y el usuario
        ThreadResponseDto response = threadService.createThread(threadRequestDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}