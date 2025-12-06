package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.AdminThreadActionDto;
import com.focalizze.Focalizze.dto.ReportResponseDto;
import com.focalizze.Focalizze.dto.SuspendRequestDto;
import com.focalizze.Focalizze.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @DeleteMapping("/delete/{username}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable String username){
        adminService.deleteAdmin(username);
        return ResponseEntity.ok().build();
    }
}
