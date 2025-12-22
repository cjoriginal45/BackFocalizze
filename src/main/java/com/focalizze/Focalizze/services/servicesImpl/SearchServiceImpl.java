package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link SearchService} interface.
 * Handles search operations for users and content (threads/categories).
 * <p>
 * Implementación de la interfaz {@link SearchService}.
 * Maneja operaciones de búsqueda para usuarios y contenido (hilos/categorías).
 */
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadRepository threadRepository;
    private final ThreadMapper threadMapper;


    /**
     * Searches for users by username prefix (autocomplete).
     * <p>
     * Busca usuarios por prefijo de nombre de usuario (autocompletado).
     *
     * @param prefix The prefix to search for (e.g., "joh").
     *               El prefijo a buscar (ej. "joh").
     * @return List of matching users (DTOs).
     *         Lista de usuarios coincidentes (DTOs).
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserSearchDto> searchUsersByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        // Remove '@' if present
        // Eliminar '@' si está presente
        String cleanPrefix = prefix.startsWith("@") ? prefix.substring(1) : prefix;

        if (cleanPrefix.isBlank()) {
            return List.of();
        }

        List<User> users = userRepository.findTop5ByUsernameStartingWithIgnoreCase(cleanPrefix);

        // Mapea a DTO
        return users.stream()
                .map(user -> new UserSearchDto(user.getUsername(), user.getDisplayName(), user.getAvatarUrl()))
                .collect(Collectors.toList());
    }


    /**
     * Searches for content based on a query string.
     * Strategy:
     * 1. Check if query matches a Category name exactly.
     * 2. If not, search Thread post content.
     * 3. Filter out results from blocked users.
     * <p>
     * Busca contenido basado en una cadena de consulta.
     * Estrategia:
     * 1. Comprobar si la consulta coincide exactamente con el nombre de una Categoría.
     * 2. Si no, buscar en el contenido de los Posts de los Hilos.
     * 3. Filtrar resultados de usuarios bloqueados.
     *
     * @param query The search query.
     *              La consulta de búsqueda.
     * @return List of threads matching the query.
     *         Lista de hilos que coinciden con la consulta.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ThreadResponseDto> searchContent(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 1. Resolve Current User & Blocked IDs
        // 1. Resolver Usuario Actual e IDs Bloqueados
        Set<Long> allBlockedIds = new HashSet<>();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User currentUser) {
            allBlockedIds.addAll(userRepository.findBlockedUserIdsByBlocker(currentUser.getId()));
            allBlockedIds.addAll(userRepository.findUserIdsWhoBlockedUser(currentUser.getId()));
        }

        // 2. Search Strategy
        // 2. Estrategia de Búsqueda
        List<ThreadClass> foundThreads;

        // A. Try Category Match / Intentar Coincidencia de Categoría
        Optional<CategoryClass> category = categoryRepository.findByName(query);

        if (category.isPresent()) {
            foundThreads = threadRepository.findByCategory(category.get());
        } else {
            foundThreads = threadRepository.findByPostContentContainingIgnoreCase(query);
        }

        // 3. Filter Blocked Users (Memory Filter)
        // 3. Filtrar Usuarios Bloqueados (Filtro en Memoria)
        List<ThreadClass> filteredThreads = foundThreads.stream()
                .filter(thread -> !allBlockedIds.contains(thread.getUser().getId()))
                .toList();

        // Mapeamos los resultados al DTO de respuesta.
        return threadMapper.toDtoList(filteredThreads);
    }

}
