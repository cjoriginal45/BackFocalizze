package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FeedService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the {@link FeedService} interface.
 * Orchestrates the aggregation of threads from followed users, categories, and self.
 * <p>
 * Implementación de la interfaz {@link FeedService}.
 * Orquesta la agregación de hilos de usuarios seguidos, categorías y propios.
 */
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;

    /**
     * Generates the "Following" feed for the authenticated user.
     * Aggregates threads, filters blocked users, and enriches data (likes, saved status).
     * <p>
     * Genera el feed "Siguiendo" para el usuario autenticado.
     * Agrega hilos, filtra usuarios bloqueados y enriquece datos (likes, estado guardado).
     *
     * @param pageable Pagination info. / Información de paginación.
     * @return A {@link Page} of enriched {@link FeedThreadDto}.
     *         Una {@link Page} de {@link FeedThreadDto} enriquecidos.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getFeed(Pageable pageable) {
        //  Get authenticated user
        // Obtener usuario autenticado
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado"));

        // Extract IDs from Lazy Collections (Fetched inside transaction)
        // Extraer IDs de Colecciones Lazy (Obtenidas dentro de la transacción)
        List<Long> followedUserIds = new ArrayList<>(
                currentUser.getFollowing().stream()
                        .map(follow -> follow.getUserFollowed().getId())
                        .toList()
        );

        List<Long> followedCategoryIds = new ArrayList<>(
                currentUser.getFollowedCategories().stream()
                        .map(categoryFollow -> categoryFollow.getCategory().getId())
                        .toList()
        );

        // Hibernate might generate invalid SQL for "IN ()". We add a dummy ID (-1L).
        // Hibernate podría generar SQL inválido para "IN ()". Agregamos un ID ficticio (-1L).
        if (followedUserIds.isEmpty()) {
            followedUserIds.add(-1L);
        }
        if (followedCategoryIds.isEmpty()) {
            followedCategoryIds.add(-1L);
        }

        // --- BLOCKED USERS HANDLING / MANEJO DE USUARIOS BLOQUEADOS ---
        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.addAll(userRepository.findBlockedUserIdsByBlocker(currentUser.getId()));
        allBlockedIds.addAll(userRepository.findUserIdsWhoBlockedUser(currentUser.getId()));

        if (allBlockedIds.isEmpty()) {
            allBlockedIds.add(-1L); // SQL Safety / Seguridad SQL
        }

        // Query the Repository (Includes Self-Threads logic)
        // Consultar el Repositorio (Incluye lógica de Hilos Propios)
        Page<ThreadClass> threadPage = threadRepository.findFollowingFeed(
                followedUserIds,
                followedCategoryIds,
                currentUser.getId(),
                allBlockedIds,
                pageable
        );

        List<ThreadClass> threadsOnPage = threadPage.getContent();
        List<FeedThreadDto> enrichedContent = threadEnricher.enrichList(threadsOnPage, currentUser);

        return new PageImpl<>(enrichedContent, pageable, threadPage.getTotalElements());
    }
}
