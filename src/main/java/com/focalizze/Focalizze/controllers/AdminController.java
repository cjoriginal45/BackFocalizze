package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.AdminThreadActionDto;
import com.focalizze.Focalizze.dto.PromoteAdminDto;
import com.focalizze.Focalizze.dto.ReportResponseDto;
import com.focalizze.Focalizze.dto.SuspendRequestDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {
    private final AdminService adminService;

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
    public ResponseEntity<Void> promoteToAdmin(
            @Valid @RequestBody PromoteAdminDto request,
            @AuthenticationPrincipal User currentAdmin
    ) {
        adminService.promoteUserToAdmin(request, currentAdmin);
        return ResponseEntity.ok().build();
    }
}
