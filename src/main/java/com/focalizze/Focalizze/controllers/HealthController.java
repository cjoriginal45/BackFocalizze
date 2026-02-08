package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/health")
public class HealthController {

    private final UserRepository userRepository;

    @GetMapping
    public String checkHealth() {
        // TRUCO: Hacemos una consulta ultra-ligera a la DB
        // Esto obliga a Hibernate a abrir conexión y evita que Aiven se duerma.
        long count = userRepository.count();

        return "Backend OK. Usuarios en DB: " + count;
    }
}
