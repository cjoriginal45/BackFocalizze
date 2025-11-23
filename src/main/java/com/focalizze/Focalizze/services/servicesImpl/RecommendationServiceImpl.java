package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.RecommendationReasonType;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;

    // Pesos para el algoritmo de scoring
    private static final double LIKE_WEIGHT = 0.5;
    private static final double COMMENT_WEIGHT = 1.5;
    private static final double SAVE_WEIGHT = 2.0;
    private static final double RECENCY_WEIGHT = 1.2;

    @Override
    @Transactional(readOnly = true)
    public List<DiscoverItemDto> getRecommendations(User currentUser, int limit) {
        // 1. Obtener IDs (Tu código actual) ...
        User userWithFollows = userRepository.findByIdWithFollows(currentUser.getId()).orElse(currentUser);
        List<Long> followedUserIds = userWithFollows.getFollowing().stream()
                .map(f -> f.getUserFollowed().getId()).toList();
        List<Long> followedCategoryIds = userWithFollows.getFollowedCategories().stream()
                .map(cf -> cf.getCategory().getId()).toList();

        // 2. Selección de Candidatos (Tu código actual) ...
        List<ThreadClass> candidates = threadRepository.findRecommendationCandidates(
                currentUser.getId(), followedUserIds, followedCategoryIds, PageRequest.of(0, 100)
        );

        // 3. Scoring (Tu código actual) ...
        List<ScoredThread> scoredThreads = candidates.stream()
                .map(this::calculateScore)
                .sorted(Comparator.comparingDouble(ScoredThread::score).reversed())
                .toList();

        // 4. Diversificación (Tu código actual) ...
        List<DiscoverItemDto> recommendations = new ArrayList<>();
        Set<Long> usedAuthors = new HashSet<>();

        for (ScoredThread scoredThread : scoredThreads) {
            if (recommendations.size() >= limit) break;
            ThreadClass thread = scoredThread.thread();

            if (!usedAuthors.contains(thread.getUser().getId())) {
                usedAuthors.add(thread.getUser().getId());

                // Lógica de Razón (Tu código actual)
                String reason = "Basado en interacciones recientes de tu red.";
                RecommendationReasonType type = RecommendationReasonType.SOCIAL_PROOF;

                if (followedCategoryIds.contains(thread.getCategory().getId())) {
                    reason = "Porque sigues la categoría '" + thread.getCategory().getName() + "'.";
                    type = RecommendationReasonType.CATEGORY_INTEREST;
                }

                FeedThreadDto enrichedThread = threadEnricher.enrichList(List.of(thread), currentUser).get(0);
                recommendations.add(new DiscoverItemDto(enrichedThread, true, reason, type.toString()));
            }
        }

        // --- NUEVO: FALLBACK (PLAN B) ---
        // Si no encontramos suficientes recomendaciones personalizadas, rellenamos con hilos populares.
        if (recommendations.size() < limit) {
            // Buscamos hilos que NO sean del usuario ni de sus seguidos (Globales)
            Page<ThreadClass> fallbackThreads = threadRepository.findThreadsForDiscover(
                    currentUser.getId(), followedUserIds, PageRequest.of(0, 10)
            );

            for (ThreadClass thread : fallbackThreads) {
                if (recommendations.size() >= limit) break;
                // Evitamos duplicados
                boolean alreadyAdded = recommendations.stream().anyMatch(r -> r.thread().id().equals(thread.getId()));

                if (!alreadyAdded && !usedAuthors.contains(thread.getUser().getId())) {
                    FeedThreadDto enriched = threadEnricher.enrichList(List.of(thread), currentUser).get(0);
                    recommendations.add(new DiscoverItemDto(
                            enriched, true, "Tendencia en Focalizze.", "TRENDING"
                    ));
                }
            }
        }
        // -------------------------------

        return recommendations;
    }

    private ScoredThread calculateScore(ThreadClass thread) {
        double engagementScore = (thread.getLikeCount() * LIKE_WEIGHT) +
                (thread.getCommentCount() * COMMENT_WEIGHT) +
                (thread.getSaveCount() * SAVE_WEIGHT);

        // Penalización por antigüedad (más antiguo, menor puntuación)
        long hoursOld = Duration.between(thread.getPublishedAt(), LocalDateTime.now()).toHours();
        double recencyScore = Math.exp(-hoursOld * 0.01) * RECENCY_WEIGHT; // Decaimiento exponencial

        double finalScore = engagementScore * recencyScore;
        return new ScoredThread(thread, finalScore);
    }

    // Record auxiliar para el scoring
    private record ScoredThread(ThreadClass thread, double score) {}
}

