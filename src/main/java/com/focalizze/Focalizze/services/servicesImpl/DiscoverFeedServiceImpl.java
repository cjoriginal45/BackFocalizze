package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.DiscoverFeedService;
import com.focalizze.Focalizze.services.RecommendationService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the {@link DiscoverFeedService} interface.
 * Generates a mixed feed containing organic content and personalized recommendations.
 * <p>
 * Implementación de la interfaz {@link DiscoverFeedService}.
 * Genera un feed mixto que contiene contenido orgánico y recomendaciones personalizadas.
 */
@Service
@RequiredArgsConstructor
public class DiscoverFeedServiceImpl implements DiscoverFeedService {

    private final ThreadRepository threadRepository;
    private final RecommendationService recommendationService;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;

    // Configuration: 1 recommendation inserted every 3 normal items
    // Configuración: 1 recomendación insertada cada 3 ítems normales
    private static final int INSERTION_RATE = 3;

    /**
     * Retrieves a paginated discover feed.
     * The strategy involves fetching organic threads (excluding followed users) and interleaving recommendations.
     * <p>
     * Recupera un feed de descubrimiento paginado.
     * La estrategia implica obtener hilos orgánicos (excluyendo usuarios seguidos) e intercalar recomendaciones.
     *
     * @param currentUser The user requesting the feed.
     *                    El usuario que solicita el feed.
     * @param pageable    Pagination info.
     *                    Información de paginación.
     * @return A {@link Page} of {@link DiscoverItemDto} containing both threads and recommendations.
     *         Una {@link Page} de {@link DiscoverItemDto} conteniendo tanto hilos como recomendaciones.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DiscoverItemDto> getDiscoverFeed(User currentUser, Pageable pageable) {
        // 1. Get Followed User IDs (To exclude them from "Discover" feed)
        // 1. Obtener IDs de Usuarios Seguidos (Para excluirlos del feed "Discover")
        User userWithFollows = userRepository.findByIdWithFollows(currentUser.getId()).orElse(currentUser);
        List<Long> followedUserIds = userWithFollows.getFollowing().stream()
                .map(f -> f.getUserFollowed().getId()).toList();

        // 2. Blocked Logic (Consolidated)
        // 2. Lógica de Bloqueos (Consolidada)
        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.addAll(userRepository.findBlockedUserIdsByBlocker(currentUser.getId()));
        allBlockedIds.addAll(userRepository.findUserIdsWhoBlockedUser(currentUser.getId()));

        if (allBlockedIds.isEmpty()) {
            allBlockedIds.add(-1L);
        }

        // 3. Fetch "Normal Threads" (Base of the Discover Feed)
        // 3. Obtener "Hilos Normales" (Base del Feed de Descubrimiento)
        Page<ThreadClass> normalPage = threadRepository.findThreadsForDiscover(
                currentUser.getId(), followedUserIds,allBlockedIds, pageable
        );

        List<FeedThreadDto> normalDtos = threadEnricher.enrichList(normalPage.getContent(), currentUser);

        // 4. Calculate required recommendations
        // 4. Calcular recomendaciones requeridas
        int recommendationsNeeded = (normalDtos.size() / INSERTION_RATE) + 2; // Pedimos un par extra por si acaso

        // 4. Obtener recomendaciones (sigue igual)
        List<DiscoverItemDto> recommendations = recommendationService.getRecommendations(currentUser, recommendationsNeeded);

        // 5. MIX THE LISTS / MEZCLAR LAS LISTAS
        // Optimization: Pre-allocate capacity to prevent array copying during insertion
        // Optimización: Pre-asignar capacidad para prevenir copia de arrays durante la inserción
        List<DiscoverItemDto> mixedFeed = new ArrayList<>(normalDtos.size() + recommendations.size());
        int recIndex = 0;

        for (int i = 0; i < normalDtos.size(); i++) {
            // Add normal thread / Añadir hilo normal
            mixedFeed.add(new DiscoverItemDto(normalDtos.get(i), false, null, null));

            // Insert recommendation every 'INSERTION_RATE' normal threads
            // Insertar recomendación cada 'INSERTION_RATE' hilos normales
            if ((i + 1) % INSERTION_RATE == 0 && recIndex < recommendations.size()) {
                mixedFeed.add(recommendations.get(recIndex++));
            }
        }

        // Return PageImpl maintaining the total elements from the main query for pagination logic
        // Devolver PageImpl manteniendo el total de elementos de la consulta principal para la lógica de paginación
        return new PageImpl<>(mixedFeed, pageable, normalPage.getTotalElements());
    }
}