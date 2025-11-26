package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ForgotPasswordRequest;
import com.focalizze.Focalizze.dto.ResetPasswordRequest;
import com.focalizze.Focalizze.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    // FASE 1
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request.email());
        return ResponseEntity.ok().build();
    }

    // FASE 2
    @GetMapping("/validate-reset-token")
    public ResponseEntity<Void> validateToken(@RequestParam String token) {
        if (passwordResetService.validateResetToken(token)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // FASE 3
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

