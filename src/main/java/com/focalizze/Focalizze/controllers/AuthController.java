package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.RegisterResponse;
import com.focalizze.Focalizze.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
