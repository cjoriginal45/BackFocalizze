package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
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

    private static final int INSERTION_RATE = 10; // 1 recomendado cada 10 normales

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoverItemDto> getDiscoverFeed(User currentUser, Pageable pageable) {
        // 1. Obtenemos una página de hilos normales del feed público
        Page<ThreadClass> normalThreadsPage = threadRepository.findThreadsForFeed(pageable);
        List<FeedThreadDto> normalDtos = threadEnricher.enrichList(normalThreadsPage.getContent(), currentUser);

        // 2. Calculamos cuántas recomendaciones necesitamos para esta página
        int recommendationsNeeded = normalDtos.size() / INSERTION_RATE;
        List<DiscoverItemDto> recommendedItems = recommendationService.getRecommendations(currentUser, recommendationsNeeded);

        // 3. MEZCLAMOS ambas listas
        List<DiscoverItemDto> mixedFeed = new ArrayList<>();
        int normalIndex = 0;
        int recommendedIndex = 0;

        for (int i = 0; i < normalDtos.size(); i++) {
            // Insertamos un recomendado en la posición 10, 20, 30, etc.
            if ((i + 1) % (INSERTION_RATE + 1) == 0 && recommendedIndex < recommendedItems.size()) {
                mixedFeed.add(recommendedItems.get(recommendedIndex++));
            } else {
                mixedFeed.add(new DiscoverItemDto(normalDtos.get(normalIndex++), false, null, null));
            }
        }

        return new PageImpl<>(mixedFeed, pageable, normalThreadsPage.getTotalElements());
    }
}
