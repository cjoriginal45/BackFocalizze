package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.AdminService;
import com.focalizze.Focalizze.services.BackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final BackupService backupService;

    @GetMapping("/reports/users") // Endpoint específico para usuarios
    public ResponseEntity<Page<ReportResponseDto>> getUserReports(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingReports(pageable));
    }

    @GetMapping("/reports/threads")
    public ResponseEntity<Page<ReportResponseDto>> getThreadReports(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingThreadReports(pageable));
    }

    @PostMapping("/process-thread")
    public ResponseEntity<Void> processThreadReport(@RequestBody AdminThreadActionDto request) {
        adminService.processThreadReport(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/suspend")
    public ResponseEntity<Void> processSuspension(@RequestBody SuspendRequestDto request) {
        adminService.processReport(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/promote")
    public ResponseEntity<?> promoteToAdmin(
            @Valid @RequestBody PromoteAdminDto request,
            @AuthenticationPrincipal User currentAdmin
    ) {
        try {
            adminService.promoteUserToAdmin(request, currentAdmin);
            return ResponseEntity.ok().build();

        } catch (IllegalStateException e) {
            // Capturamos: "El usuario ya es Administrador"
            // Devolvemos 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));

        } catch (BadCredentialsException e) {
            // Capturamos: Contraseña incorrecta
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (UsernameNotFoundException e) {
            // Capturamos: Usuario no encontrado
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokeAdminRole(
            @Valid @RequestBody RevokeAdminDto request,
            @AuthenticationPrincipal User currentAdmin
    ) {
        try {
            adminService.revokeAdminRole(request, currentAdmin);
            return ResponseEntity.ok().build();

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/ban")
    public ResponseEntity<?> banUser(
            @Valid @RequestBody BanUserRequestDto request,
            @AuthenticationPrincipal User currentAdmin
    ) {
        try {
            adminService.banUser(request, currentAdmin);
            return ResponseEntity.ok().build();

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/backup/download")
    public ResponseEntity<InputStreamResource> downloadBackup() {
        try {
            ByteArrayInputStream in = backupService.generateExcelBackup();

            String filename = "focalizze_data_" + LocalDate.now() + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + filename);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}