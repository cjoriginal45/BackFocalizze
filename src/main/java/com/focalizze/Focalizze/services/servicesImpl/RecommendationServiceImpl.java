package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.RecommendationReasonType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.BlockService;
import com.focalizze.Focalizze.services.FeedbackService;
import com.focalizze.Focalizze.services.RecommendationService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link RecommendationService} interface.
 * Generates personalized thread recommendations based on user interests and social proof scoring.
 * <p>
 * Implementación de la interfaz {@link RecommendationService}.
 * Genera recomendaciones de hilos personalizadas basadas en intereses del usuario y puntaje de prueba social.
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;
    private final FeedbackService feedbackService;
    private final BlockRepository blockRepository;

    // Scoring weights / Pesos de puntuación
    private static final double LIKE_WEIGHT = 0.5;
    private static final double COMMENT_WEIGHT = 1.5;
    private static final double SAVE_WEIGHT = 2.0;
    private static final double RECENCY_WEIGHT = 1.2;

    /**
     * Calculates and returns a list of recommended threads.
     * <p>
     * Calcula y devuelve una lista de hilos recomendados.
     *
     * @param currentUser The user to generate recommendations for.
     *                    El usuario para quien generar recomendaciones.
     * @param limit       The maximum number of recommendations.
     *                    El número máximo de recomendaciones.
     * @return List of {@link DiscoverItemDto}.
     *         Lista de {@link DiscoverItemDto}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DiscoverItemDto> getRecommendations(User currentUser, int limit) {
        // 1. Gather Context Data / Recopilar Datos de Contexto
        User userWithFollows = userRepository.findByIdWithFollows(currentUser.getId()).orElse(currentUser);

        // Collect mutable lists for potential "-1L" insertion
        List<Long> followedUserIds = userWithFollows.getFollowing().stream()
                .map(f -> f.getUserFollowed().getId())
                .collect(Collectors.toCollection(ArrayList::new));

        List<Long> followedCategoryIds = userWithFollows.getFollowedCategories().stream()
                .map(cf -> cf.getCategory().getId())
                .collect(Collectors.toCollection(ArrayList::new));

        Set<Long> hiddenThreadIds = new HashSet<>(feedbackService.getHiddenThreadIds(currentUser));

        Set<Long> allBlockedIds = new HashSet<>();
        allBlockedIds.addAll(blockRepository.findBlockedUserIdsByBlocker(currentUser.getId()));
        allBlockedIds.addAll(blockRepository.findUserIdsWhoBlockedUser(currentUser.getId()));

        // SQL Safety: Insert dummy ID to prevent empty IN clause errors
        if (followedUserIds.isEmpty()) followedUserIds.add(-1L);
        if (followedCategoryIds.isEmpty()) followedCategoryIds.add(-1L);
        if (hiddenThreadIds.isEmpty()) hiddenThreadIds.add(-1L);
        if (allBlockedIds.isEmpty()) allBlockedIds.add(-1L);

        // Fetch Candidates / Obtener Candidatos
        List<ThreadClass> candidates = threadRepository.findRecommendationCandidates(
                currentUser.getId(),
                followedUserIds,
                followedCategoryIds,
                hiddenThreadIds,
                allBlockedIds,
                PageRequest.of(0, 100)
        );

        // Score and Rank / Puntuar y Clasificar
        List<ScoredThread> scoredThreads = candidates.stream()
                .map(this::calculateScore)
                .sorted(Comparator.comparingDouble(ScoredThread::score).reversed())
                .toList();

        // Diversification & Reason Assignment / Diversificación y Asignación de Razón
        List<DiscoverItemDto> recommendations = new ArrayList<>();
        Set<Long> usedAuthors = new HashSet<>();

        for (ScoredThread scoredThread : scoredThreads) {
            if (recommendations.size() >= limit) break;

            ThreadClass thread = scoredThread.thread();

            // Filtramos si ya usamos el autor
            if (!usedAuthors.contains(thread.getUser().getId())) {
                usedAuthors.add(thread.getUser().getId());

                // Determinamos la razón
                String reason = "Basado en la popularidad y tus intereses.";
                RecommendationReasonType type = RecommendationReasonType.SOCIAL_PROOF; // Default

                // Si la categoría del hilo está en mis seguidas
                // Nota: Usamos contains original, cuidado con el -1 si lo agregaste arriba, pero aquí comparamos IDs reales
                boolean followsCategory = userWithFollows.getFollowedCategories().stream()
                        .anyMatch(cf -> cf.getCategory().getId().equals(thread.getCategory().getId()));

                if (followsCategory) {
                    reason = "Porque sigues la categoría '" + thread.getCategory().getName() + "'.";
                    type = RecommendationReasonType.CATEGORY_INTEREST;
                }

                FeedThreadDto enrichedThread = threadEnricher.enrichList(List.of(thread), currentUser).get(0);
                recommendations.add(new DiscoverItemDto(enrichedThread, true, reason, type.toString()));
            }
        }

        // 5. Fallback Strategy / Estrategia de Respaldo
        if (recommendations.size() < limit) {
            // Fill remaining slots with trending content (filtered manually)
            // Llenar espacios restantes con contenido en tendencia (filtrado manualmente)
            Page<ThreadClass> fallbackThreads = threadRepository.findThreadsForDiscover(
                    currentUser.getId(), followedUserIds, allBlockedIds, PageRequest.of(0, 20)
            );

            for (ThreadClass thread : fallbackThreads) {
                if (recommendations.size() >= limit) break;

                // Verificamos que no esté oculto, que no sea propio y que no esté ya agregado
                boolean isHidden = hiddenThreadIds.contains(thread.getId());
                boolean alreadyAdded = recommendations.stream().anyMatch(r -> r.thread().id().equals(thread.getId()));

                if (!isHidden && !alreadyAdded && !thread.getUser().getId().equals(currentUser.getId())) {
                    FeedThreadDto enriched = threadEnricher.enrichList(List.of(thread), currentUser).get(0);

                    recommendations.add(new DiscoverItemDto(
                            enriched,
                            true,
                            "Tendencia en Focalizze.",
                            "TRENDING"
                    ));
                }
            }
        }

        return recommendations;
    }

    /**
     * Internal scoring algorithm.
     * <p>
     * Algoritmo de puntuación interno.
     */
    private ScoredThread calculateScore(ThreadClass thread) {
        double engagementScore = (thread.getLikeCount() * LIKE_WEIGHT) +
                (thread.getCommentCount() * COMMENT_WEIGHT) +
                (thread.getSaveCount() * SAVE_WEIGHT);

        long hoursOld = Duration.between(thread.getPublishedAt(), LocalDateTime.now()).toHours();
        // Evitamos números negativos o ceros extraños en logs recientes
        hoursOld = Math.max(0, hoursOld);

        double recencyScore = Math.exp(-hoursOld * 0.01) * RECENCY_WEIGHT;

        double finalScore = engagementScore * recencyScore;
        return new ScoredThread(thread, finalScore);
    }

    private record ScoredThread(ThreadClass thread, double score) {}
}

