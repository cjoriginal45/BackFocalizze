package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.TwoFactorRequestDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    // PATCH: /api/security/2fa
    @PatchMapping("/2fa")
    public ResponseEntity<Void> toggleTwoFactor(
            @RequestBody TwoFactorRequestDto request,
            @AuthenticationPrincipal User currentUser
    ) {
        securityService.toggleTwoFactor(request.enabled(), currentUser);
        return ResponseEntity.ok().build();
    }

    // POST: /api/security/logout-all
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllDevices(
            @AuthenticationPrincipal User currentUser
    ) {
        securityService.logoutAllDevices(currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate-password")
    public ResponseEntity<Boolean> validatePassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User currentUser
    ) {
        String password = request.get("password");
        boolean isValid = securityService.validatePassword(password, currentUser);

        if (isValid) {
            return ResponseEntity.ok(true);
        } else {
            // Devolvemos 400 o 403 si está mal
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
        }
    }
}