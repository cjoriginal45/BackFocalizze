package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.services.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    // Endpoint para la búsqueda predictiva de usuarios
    @GetMapping("/users")
    public ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam("q") String query) {
        System.out.println("se esta ejecutando");
        System.out.println("query: "+query);
        List<UserSearchDto> results = searchService.searchUsersByPrefix(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/content")
    public ResponseEntity<List<ThreadResponseDto>> searchContent(@RequestParam("q") String query) {
        List<ThreadResponseDto> results = searchService.searchContent(query);
        return ResponseEntity.ok(results);
    }


}
