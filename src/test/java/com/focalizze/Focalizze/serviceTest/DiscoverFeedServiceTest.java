package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.RecommendationService;
import com.focalizze.Focalizze.services.servicesImpl.DiscoverFeedServiceImpl;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DiscoverFeedServiceTest {
    @Mock private ThreadRepository threadRepository;
    @Mock private RecommendationService recommendationService;
    @Mock private ThreadEnricher threadEnricher;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private DiscoverFeedServiceImpl discoverFeedService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("user").following(new ArrayList<>()).build();
    }

    @Test
    @DisplayName("getDiscoverFeed: Debería mezclar hilos normales con recomendaciones")
    void getDiscoverFeed_ShouldMixContent() {
        // Given
        // 1. Mock de User y Follows
        given(userRepository.findByIdWithFollows(1L)).willReturn(Optional.of(currentUser));

        // 2. Mock de Bloqueos (Vacíos)
        given(userRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Collections.emptySet());
        given(userRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Collections.emptySet());

        // 3. Mock de Hilos Normales (Simulamos 6 hilos normales)
        List<ThreadClass> threads = createThreads(6);
        Page<ThreadClass> page = new PageImpl<>(threads);

        // Mock de Enriquecimiento (Convierte ThreadClass a DTO)
        List<FeedThreadDto> dtos = createThreadDtos(6);
        given(threadEnricher.enrichList(anyList(), eq(currentUser))).willReturn(dtos);

        given(threadRepository.findThreadsForDiscover(
                eq(1L), anyList(), anySet(), any(Pageable.class)
        )).willReturn(page);

        // 4. Mock de Recomendaciones (Simulamos 3 recomendaciones)
        List<DiscoverItemDto> recommendations = createRecommendationDtos(3);
        given(recommendationService.getRecommendations(eq(currentUser), anyInt())).willReturn(recommendations);

        // When
        Page<DiscoverItemDto> result = discoverFeedService.getDiscoverFeed(currentUser, Pageable.unpaged());

        // Then
        List<DiscoverItemDto> content = result.getContent();

        // Verificamos el tamaño total: 6 normales + recomendaciones insertadas
        // Con rate 3: N, N, N, R, N, N, N, R
        // Total esperado: 6 + 2 = 8 items
        assertThat(content).hasSize(8);

        // Verificamos posiciones de inserción (Rate = 3)
        // Indices: 0,1,2 (Normal), 3 (Recomendado), 4,5,6 (Normal), 7 (Recomendado)
        assertThat(content.get(3).isRecommended()).isTrue();
        assertThat(content.get(7).isRecommended()).isTrue();

        // Verificamos que los otros son normales
        assertThat(content.get(0).isRecommended()).isFalse();
        assertThat(content.get(2).isRecommended()).isFalse();
    }

    @Test
    @DisplayName("getDiscoverFeed: Debería manejar lista de recomendaciones vacía")
    void getDiscoverFeed_NoRecommendations_ShouldReturnOnlyNormal() {
        // Given
        given(userRepository.findByIdWithFollows(1L)).willReturn(Optional.of(currentUser));
        given(userRepository.findBlockedUserIdsByBlocker(1L)).willReturn(Collections.emptySet());
        given(userRepository.findUserIdsWhoBlockedUser(1L)).willReturn(Collections.emptySet());

        List<ThreadClass> threads = createThreads(5);
        Page<ThreadClass> page = new PageImpl<>(threads);
        List<FeedThreadDto> dtos = createThreadDtos(5);

        given(threadRepository.findThreadsForDiscover(anyLong(), anyList(), anySet(), any())).willReturn(page);
        given(threadEnricher.enrichList(anyList(), any())).willReturn(dtos);

        // Mock: Recomendaciones vacías
        given(recommendationService.getRecommendations(any(), anyInt())).willReturn(Collections.emptyList());

        // When
        Page<DiscoverItemDto> result = discoverFeedService.getDiscoverFeed(currentUser, Pageable.unpaged());

        // Then
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent()).allMatch(item -> !item.isRecommended());
    }

    // --- Helpers ---

    private List<ThreadClass> createThreads(int count) {
        List<ThreadClass> list = new ArrayList<>();
        for(int i=0; i<count; i++) list.add(new ThreadClass());
        return list;
    }

    private List<FeedThreadDto> createThreadDtos(int count) {
        List<FeedThreadDto> list = new ArrayList<>();
        for(int i=0; i<count; i++) {
            // Mock básico de DTO
            list.add(new FeedThreadDto((long)i, null, null, null, null, false, false, "Cat"));
        }
        return list;
    }

    private List<DiscoverItemDto> createRecommendationDtos(int count) {
        List<DiscoverItemDto> list = new ArrayList<>();
        for(int i=0; i<count; i++) {
            list.add(new DiscoverItemDto(null, true, "Reason", "TYPE"));
        }
        return list;
    }
}
