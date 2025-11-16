package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.services.CategoryFollowService;
import com.focalizze.Focalizze.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryFollowService categoryFollowService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<CategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleFollow(@PathVariable Long id){
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        categoryFollowService.toggleFollowCategory(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{name}")
    public ResponseEntity<CategoryDetailsDto> getCategoryDetails(@PathVariable String name) {
        return ResponseEntity.ok(categoryService.getCategoryDetails(name));
    }

    @GetMapping("/{name}/threads")
    public ResponseEntity<Page<FeedThreadDto>> getThreadsByCategory(
            @PathVariable String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(categoryService.getThreadsByCategory(name, pageable));
    }
}
