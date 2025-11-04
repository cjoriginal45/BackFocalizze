package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.services.SaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saved-threads")
@RequiredArgsConstructor
public class SavedThreadsController {
    private final SaveService saveService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FeedThreadDto>> getSavedThreads(
            @PageableDefault(size = 10) Pageable pageable){
        Page<FeedThreadDto> savedThreadsPage = saveService.getSavedThreadsForCurrentUser(pageable);
        return ResponseEntity.ok(savedThreadsPage);
    }
}
