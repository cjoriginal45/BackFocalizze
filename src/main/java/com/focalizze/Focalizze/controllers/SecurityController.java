package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.TwoFactorRequestDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}