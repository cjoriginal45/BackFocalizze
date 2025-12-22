package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.AdminService;
import com.focalizze.Focalizze.services.BackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Controller for Administrative operations.
 * Restricted to users with the 'ROLE_ADMIN' authority.
 * <p>
 * Controlador para operaciones administrativas.
 * Restringido a usuarios con la autoridad 'ROLE_ADMIN'.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Slf4j
public class AdminController {
    private final AdminService adminService;
    private final BackupService backupService;

    /**
     * Retrieves a paginated list of pending user reports (profile reports).
     * <p>
     * Recupera una lista paginada de reportes de usuario pendientes (reportes de perfil).
     *
     * @param pageable Pagination info. / Información de paginación.
     * @return Response containing the page of reports. / Respuesta conteniendo la página de reportes.
     */
    @GetMapping("/reports/users") // Endpoint específico para usuarios
    public ResponseEntity<Page<ReportResponseDto>> getUserReports(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingReports(pageable));
    }

    /**
     * Retrieves a paginated list of pending thread reports.
     * <p>
     * Recupera una lista paginada de reportes de hilos pendientes.
     *
     * @param pageable Pagination info. / Información de paginación.
     * @return Response containing the page of thread reports. / Respuesta conteniendo la página de reportes de hilos.
     */
    @GetMapping("/reports/threads")
    public ResponseEntity<Page<ReportResponseDto>> getThreadReports(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingThreadReports(pageable));
    }

    /**
     * Processes a specific thread report (Dismiss, Delete, Edit).
     * <p>
     * Procesa un reporte de hilo específico (Descartar, Eliminar, Editar).
     *
     * @param request The action details. / Los detalles de la acción.
     * @return Empty response (200 OK). / Respuesta vacía (200 OK).
     */
    @PostMapping("/process-thread")
    public ResponseEntity<Void> processThreadReport(@RequestBody AdminThreadActionDto request) {
        adminService.processThreadReport(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Processes a user suspension request based on a report.
     * <p>
     * Procesa una solicitud de suspensión de usuario basada en un reporte.
     *
     * @param request The suspension details. / Los detalles de la suspensión.
     * @return Empty response (200 OK). / Respuesta vacía (200 OK).
     */
    @PostMapping("/suspend")
    public ResponseEntity<Void> processSuspension(@RequestBody SuspendRequestDto request) {
        adminService.processReport(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Promotes a user to the Administrator role.
     * <p>
     * Promueve a un usuario al rol de Administrador.
     *
     * @param request      The promotion request dto. / El dto de solicitud de promoción.
     * @param currentAdmin The authenticated admin performing the action. / El administrador autenticado que realiza la acción.
     * @return Response entity. / Entidad de respuesta.
     */
    @PostMapping("/promote")
    public ResponseEntity<?> promoteToAdmin(
            @Valid @RequestBody PromoteAdminDto request,
            @AuthenticationPrincipal User currentAdmin
    ) {
        try {
            adminService.promoteUserToAdmin(request, currentAdmin);
            return ResponseEntity.ok().build();

        } catch (IllegalStateException e) {
            // 409 Conflict: User is already admin
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));

        } catch (BadCredentialsException e) {
            // 403 Forbidden: Wrong password
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (UsernameNotFoundException e) {
            // 404 Not Found: User not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Revokes the Administrator role from a user.
     * <p>
     * Revoca el rol de Administrador de un usuario.
     *
     * @param request      The revoke request dto. / El dto de solicitud de revocación.
     * @param currentAdmin The authenticated admin. / El administrador autenticado.
     * @return Response entity. / Entidad de respuesta.
     */
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

    /**
     * Bans a user from the platform.
     * <p>
     * Banea a un usuario de la plataforma.
     *
     * @param request      The ban details. / Los detalles del baneo.
     * @param currentAdmin The authenticated admin. / El administrador autenticado.
     * @return Response entity. / Entidad de respuesta.
     */
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

    /**
     * Generates and downloads an Excel backup of the system data.
     * <p>
     * Genera y descarga una copia de seguridad en Excel de los datos del sistema.
     *
     * @return The Excel file as a resource. / El archivo Excel como un recurso.
     */
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
            log.error("Error generating backup / Error generando backup", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}