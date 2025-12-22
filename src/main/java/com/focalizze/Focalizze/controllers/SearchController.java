package com.focalizze.Focalizze.controllers;

import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.services.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for global search functionality.
 * Handles predictive user search and content search.
 * <p>
 * Controlador para la funcionalidad de búsqueda global.
 * Maneja la búsqueda predictiva de usuarios y la búsqueda de contenido.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    private final SearchService searchService;

    /**
     * Predictive search for users by username prefix.
     * <p>
     * Búsqueda predictiva de usuarios por prefijo de nombre de usuario.
     *
     * @param query The search query prefix. / El prefijo de la consulta de búsqueda.
     * @return List of matching users. / Lista de usuarios coincidentes.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam("q") String query) {
        log.debug("Executing user search with query: {}", query);
        List<UserSearchDto> results = searchService.searchUsersByPrefix(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Searches for threads/content based on a text query.
     * <p>
     * Busca hilos/contenido basado en una consulta de texto.
     *
     * @param query The search query. / La consulta de búsqueda.
     * @return List of matching threads. / Lista de hilos coincidentes.
     */
    @GetMapping("/content")
    public ResponseEntity<List<ThreadResponseDto>> searchContent(@RequestParam("q") String query) {
        List<ThreadResponseDto> results = searchService.searchContent(query);
        return ResponseEntity.ok(results);
    }


}
