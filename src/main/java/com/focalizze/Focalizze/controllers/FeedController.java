package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.DiscoverFeedService;
import com.focalizze.Focalizze.services.FeedService;
import com.focalizze.Focalizze.services.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    private final FeedService feedService;
    private final DiscoverFeedService discoverFeedService;
    private final FeedbackService feedbackService;

    public FeedController(FeedService feedService, DiscoverFeedService discoverFeedService, FeedbackService feedbackService) {
        this.feedService = feedService;
        this.discoverFeedService = discoverFeedService;
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public ResponseEntity<Page<FeedThreadDto>> getFeed(
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(feedService.getFeed(pageable));
    }

    @GetMapping("/discover")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DiscoverItemDto>> getDiscoverFeed(
            @PageableDefault(size = 20) Pageable pageable) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(discoverFeedService.getDiscoverFeed(currentUser, pageable));
    }

    @PostMapping("/feedback/hide")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> hideThread(
            @RequestParam Long threadId,
            @RequestParam String reasonType) {

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        feedbackService.hideThread(threadId, reasonType, currentUser);
        return ResponseEntity.ok().build();
    }
}