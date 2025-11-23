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
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscoverFeedServiceImpl implements DiscoverFeedService {

    private final ThreadRepository threadRepository;
    private final RecommendationService recommendationService;
    private final ThreadEnricher threadEnricher;
    private final UserRepository userRepository;

    // Configuración: 1 recomendado cada 10 normales
    private static final int INSERTION_RATE = 3;

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoverItemDto> getDiscoverFeed(User currentUser, Pageable pageable) {
        // 1. Obtener IDs de seguidos para excluirlos del feed "General"
        // (Queremos descubrir cosas nuevas, no ver lo que ya seguimos)
        User userWithFollows = userRepository.findByIdWithFollows(currentUser.getId()).orElse(currentUser);
        List<Long> followedUserIds = userWithFollows.getFollowing().stream()
                .map(f -> f.getUserFollowed().getId()).toList();

        // 2. Obtener "Hilos Normales" (Base del Feed Discover)
        // Estos tendrán isRecommended = false
        Page<ThreadClass> normalPage = threadRepository.findThreadsForDiscover(
                currentUser.getId(), followedUserIds, pageable
        );

        List<FeedThreadDto> normalDtos = threadEnricher.enrichList(normalPage.getContent(), currentUser);

        // 3. Calcular cuántas recomendaciones necesitamos (ahora serán más)
        int recommendationsNeeded = (normalDtos.size() / INSERTION_RATE) + 2; // Pedimos un par extra por si acaso

        // 4. Obtener recomendaciones (sigue igual)
        List<DiscoverItemDto> recommendations = recommendationService.getRecommendations(currentUser, recommendationsNeeded);

        // 5. MEZCLAR LAS LISTAS
        List<DiscoverItemDto> mixedFeed = new ArrayList<>();
        int recIndex = 0;

        for (int i = 0; i < normalDtos.size(); i++) {
            mixedFeed.add(new DiscoverItemDto(normalDtos.get(i), false, null, null));

            // Insertamos una recomendación cada 'INSERTION_RATE' hilos normales
            if ((i + 1) % INSERTION_RATE == 0 && recIndex < recommendations.size()) {
                mixedFeed.add(recommendations.get(recIndex++));
            }
        }

        // Si sobraron recomendaciones y la página no está llena, podrías agregarlas al final (opcional)

        return new PageImpl<>(mixedFeed, pageable, normalPage.getTotalElements());
    }
}