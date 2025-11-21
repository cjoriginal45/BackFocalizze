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
        // 1. Obtener IDs de usuarios y categorías que el usuario actual sigue.
        User userWithFollows = userRepository.findByIdWithFollows(currentUser.getId()).orElse(currentUser);
        List<Long> followedUserIds = userWithFollows.getFollowing().stream()
                .map(followRelationship -> followRelationship.getUserFollowed().getId())
                .toList();
        List<Long> followedCategoryIds = userWithFollows.getFollowedCategories().stream().map(cf -> cf.getCategory().getId()).toList();

        // 2. SELECCIÓN DE CANDIDATOS: Buscar hilos relevantes que no sean del propio usuario ni de gente que ya sigue.
        List<ThreadClass> candidates = threadRepository.findRecommendationCandidates(
                currentUser.getId(),
                followedUserIds,
                followedCategoryIds,
                PageRequest.of(0, 100) // Buscamos en un pool grande de candidatos recientes
        );

        // 3. SCORING Y RANKING: Calcular una puntuación para cada hilo candidato.
        List<ScoredThread> scoredThreads = candidates.stream()
                .map(this::calculateScore)
                .sorted(Comparator.comparingDouble(ScoredThread::score).reversed())
                .toList();

        // 4. DIVERSIFICACIÓN Y RAZÓN: Seleccionar los mejores y evitar duplicados de autor.
        List<DiscoverItemDto> recommendations = new ArrayList<>();
        Set<Long> usedAuthors = new HashSet<>();

        for (ScoredThread scoredThread : scoredThreads) {
            if (recommendations.size() >= limit) break;

            ThreadClass thread = scoredThread.thread();
            if (!usedAuthors.contains(thread.getUser().getId())) {
                usedAuthors.add(thread.getUser().getId());

                // Determinamos la razón de la recomendación
                String reason = "Basado en la popularidad y tus intereses."; // Razón por defecto
                RecommendationReasonType type = RecommendationReasonType.CATEGORY_INTEREST;

                if (followedCategoryIds.contains(thread.getCategory().getId())) {
                    reason = "Porque sigues la categoría '" + thread.getCategory().getName() + "'.";
                }

                // Enriquecemos el hilo para el usuario actual (isLiked, isSaved)
                FeedThreadDto enrichedThread = threadEnricher.enrichList(List.of(thread), currentUser).get(0);

                recommendations.add(new DiscoverItemDto(enrichedThread, true, reason, type.toString()));
            }
        }
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

