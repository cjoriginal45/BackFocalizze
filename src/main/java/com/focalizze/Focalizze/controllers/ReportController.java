package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ReportRequestDto;
import com.focalizze.Focalizze.services.ReportService;
import com.focalizze.Focalizze.services.servicesImpl.ReportServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling user and thread reports.
 * Used by users to report inappropriate content or behavior.
 * <p>
 * Controlador para manejar reportes de usuarios e hilos.
 * Utilizado por los usuarios para reportar contenido o comportamiento inapropiado.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    /**
     * Reports a user profile.
     * <p>
     * Reporta un perfil de usuario.
     *
     * @param username The username of the user being reported.
     *                 El nombre de usuario del usuario que está siendo reportado.
     * @param request  The report details (reason, description).
     *                 Los detalles del reporte (razón, descripción).
     * @return Empty response (200 OK).
     *         Respuesta vacía (200 OK).
     */
    @PostMapping("/users/{username}")
    public ResponseEntity<Void> reportUser(
            @PathVariable String username,
            @Valid @RequestBody ReportRequestDto request) {

        reportService.reportUser(username, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Reports a specific thread.
     * <p>
     * Reporta un hilo específico.
     *
     * @param threadId The ID of the thread being reported.
     *                 El ID del hilo que está siendo reportado.
     * @param request  The report details.
     *                 Los detalles del reporte.
     * @return Empty response (200 OK).
     *         Respuesta vacía (200 OK).
     */
    @PostMapping("/threads/{threadId}")
    public ResponseEntity<Void> reportThread(
            @PathVariable Long threadId,
            @Valid @RequestBody ReportRequestDto request) {

        reportService.reportThread(threadId, request);
        return ResponseEntity.ok().build();
    }
}
