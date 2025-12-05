package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ReportRequestDto;
import com.focalizze.Focalizze.services.ReportService;
import com.focalizze.Focalizze.services.servicesImpl.ReportServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/users/{username}")
    public ResponseEntity<Void> reportUser(
            @PathVariable String username,
            @Valid @RequestBody ReportRequestDto request) {

        reportService.reportUser(username, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/threads/{threadId}")
    public ResponseEntity<Void> reportThread(
            @PathVariable Long threadId,
            @Valid @RequestBody ReportRequestDto request) {

        reportService.reportThread(threadId, request);
        return ResponseEntity.ok().build();
    }
}
