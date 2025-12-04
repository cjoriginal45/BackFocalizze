package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ReportRequestDto;
import com.focalizze.Focalizze.services.servicesImpl.ReportServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportServiceImpl reportService;

    @PostMapping("/users/{username}")
    public ResponseEntity<Void> reportUser(
            @PathVariable String username,
            @Valid @RequestBody ReportRequestDto request) {

        reportService.reportUser(username, request);
        return ResponseEntity.ok().build();
    }
}
