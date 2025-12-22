package com.focalizze.Focalizze.utils;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.dto.mappers.UserMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Component responsible for enriching Thread entities with context-aware data.
 * It populates transient fields like "isLiked", "isSaved", "isFollowing" relative to the current user.
 * <p>
 * Componente responsable de enriquecer entidades de Hilo con datos conscientes del contexto.
 * Puebla transitorios como "isLiked", "isSaved", "isFollowing" relativos al usuario actual.
 */
@Component
@RequiredArgsConstructor
public class ThreadEnricher {

    private final SavedThreadRepository savedThreadRepository;
    private final FeedMapper feedMapper;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;

    /**
     * Enriches a single Thread entity with interaction status for the current user.
     * Efficient for single item retrieval (e.g., thread details).
     * <p>
     * Enriquece una entidad de Hilo única con el estado de interacción para el usuario actual.
     * Eficiente para la recuperación de un solo ítem (ej. detalles del hilo).
     *
     * @param thread      The thread entity to enrich.
     *                    La entidad del hilo a enriquecer.
     * @param currentUser The user making the request (can be null).
     *                    El usuario que realiza la petición (puede ser null).
     * @return A fully enriched {@link FeedThreadDto}.
     *         Un {@link FeedThreadDto} completamente enriquecido.
     */
    public FeedThreadDto enrich(ThreadClass thread, User currentUser) {
        // 1. Base conversion using mapper / Conversión base usando el mapper
        FeedThreadDto baseDto = feedMapper.toFeedThreadDto(thread);

        // Fast exit for guests / Salida rápida para invitados
        if (currentUser == null) {
            return baseDto.withInteractionStatus(false, false);
        }

        // 2. Calculate 'isLiked' (In-memory if fetched eagerly)
        // 2. Calcular 'isLiked' (En memoria si se cargó ansiosamente)
        boolean isLikedByCurrentUser = thread.getLikes() != null && thread.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

        // 3. Calculate 'isSaved' (DB Query - Safe for single item)
        // 3. Calcular 'isSaved' (Consulta BD - Seguro para un solo ítem)
        boolean isSavedByCurrentUser = savedThreadRepository.existsByUserAndThread(currentUser, thread);

        // 4. Return updated DTO / Devolver DTO actualizado
        return baseDto.withInteractionStatus(isLikedByCurrentUser, isSavedByCurrentUser);
    }

    /**
     * Optimized method to enrich a LIST of threads, avoiding the N+1 SELECT problem.
     * Performs bulk fetching for saved, followed, and blocked statuses.
     * <p>
     * Método optimizado para enriquecer una LISTA de hilos, evitando el problema N+1 SELECT.
     * Realiza búsquedas por lotes para estados de guardado, seguido y bloqueado.
     *
     * @param threads     The list of threads to enrich.
     *                    La lista de hilos a enriquecer.
     * @param currentUser The user making the request.
     *                    El usuario que realiza la petición.
     * @return A list of enriched {@link FeedThreadDto}.
     *         Una lista de {@link FeedThreadDto} enriquecidos.
     */
    public List<FeedThreadDto> enrichList(List<ThreadClass> threads, User currentUser) {
        if (threads == null || threads.isEmpty()) {
            return List.of();
        }

        // 1. Handle Guest User (No interactions) / Manejar Usuario Invitado (Sin interacciones)
        if (currentUser == null) {
            return threads.stream()
                    .map(feedMapper::toFeedThreadDto)
                    .toList();
        }

        // 2. Extract IDs for bulk queries / Extraer IDs para consultas por lotes
        List<Long> threadIds = threads.stream().map(ThreadClass::getId).toList();
        Set<Long> authorIds = threads.stream().map(t -> t.getUser().getId()).collect(Collectors.toSet());

        // 3. Bulk Fetch: Saved Status / Búsqueda por lotes: Estado Guardado
        Set<Long> savedThreadIds = savedThreadRepository.findSavedThreadIdsByUserInThreadIds(currentUser, threadIds);

        // 4. Bulk Fetch: Following Status / Búsqueda por lotes: Estado Siguiendo
        // (Prevent SQL error if authorIds is empty, though unlikely if threads is not empty)
        Set<Long> followedUserIds = authorIds.isEmpty() ? Collections.emptySet() :
                followRepository.findFollowedUserIdsByFollower(currentUser, authorIds);

        // 5. Bulk Fetch: Block Status / Búsqueda por lotes: Estado Bloqueo
        Set<Long> blockedUserIds = authorIds.isEmpty() ? Collections.emptySet() :
                blockRepository.findBlockedIdsByBlockerAndBlockedIdsIn(currentUser, authorIds);


        // --- MAPPING & ENRICHMENT / MAPEO Y ENRIQUECIMIENTO ---
        return threads.stream().map(thread -> {

            // A. 'isLiked' (In-memory)
            boolean isLiked = thread.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));

            // B. 'isSaved' (O(1) lookup in Set)
            boolean isSaved = savedThreadIds.contains(thread.getId());

            // C. 'isFollowing' (O(1) lookup in Set)
            boolean isFollowing = followedUserIds.contains(thread.getUser().getId());

            // D. 'isBlocked' (O(1) lookup in Set)
            boolean isBlocked = blockedUserIds.contains(thread.getUser().getId());

            // E. Base DTO / DTO Base
            FeedThreadDto baseDto = feedMapper.toFeedThreadDto(thread);

            // F. Reconstruct UserDto with correct relationship flags
            // F. Reconstruir UserDto con banderas de relación correctas
            UserDto finalUserDto = new UserDto(
                    baseDto.user().id(),
                    baseDto.user().username(),
                    baseDto.user().displayName(),
                    baseDto.user().avatarUrl(),
                    baseDto.user().calculatedThreadCount(), isFollowing,
                    baseDto.user().followingCount(),
                    baseDto.user().followersCount(),
                    isBlocked,
                    baseDto.user().role(),
                    baseDto.user().isTwoFactorEnabled(),
                    baseDto.user().backgroundType(),
                    baseDto.user().backgroundValue()
            );

            // G. Final DTO / DTO Final
            return new FeedThreadDto(
                    baseDto.id(),
                    finalUserDto,
                    baseDto.publicationDate(),
                    baseDto.posts(),
                    baseDto.stats(),
                    isLiked,
                    isSaved,
                    baseDto.categoryName(),
                    baseDto.images()
            );

        }).collect(Collectors.toList());
    }

}
