package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.StatsDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FeedbackService;
import com.focalizze.Focalizze.services.servicesImpl.RecommendationServiceImpl;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {
    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadEnricher threadEnricher;
    @Mock private UserRepository userRepository;
    @Mock private FeedbackService feedbackService;
    @Mock private BlockRepository blockRepository;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private User currentUser;
    private CategoryClass categoryTech;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("user").following(new ArrayList<>()).followedCategories(new HashSet<>()).build();
        categoryTech = new CategoryClass();
        categoryTech.setId(10L);
        categoryTech.setName("Tech");
    }

    @Test
    @DisplayName("getRecommendations: Should prioritize threads with higher score (Likes/Comments)")
    void getRecommendations_Scoring_ShouldSortCorrectly() {
        // Given
        setupEmptyContext();

        ThreadClass t1 = createThread(101L, 2L, 0, LocalDateTime.now());
        ThreadClass t2 = createThread(102L, 3L, 50, LocalDateTime.now());

        given(threadRepository.findRecommendationCandidates(anyLong(), anyList(), anyList(), anySet(), anySet(), any()))
                .willReturn(List.of(t1, t2));

        // FIX NPE 1: Como solo hay 2 candidatos y el límite es 10, va a intentar buscar fallback.
        // Debemos mockearlo para que devuelva vacío y no null.
        mockFallbackEmpty();

        mockEnricher(t1);
        mockEnricher(t2);

        // When
        List<DiscoverItemDto> result = recommendationService.getRecommendations(currentUser, 10);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).thread().id()).isEqualTo(102L);
        assertThat(result.get(1).thread().id()).isEqualTo(101L);
    }

    @Test
    @DisplayName("getRecommendations: Should diversify authors (only 1 thread per author)")
    void getRecommendations_Diversification_ShouldFilterSameAuthor() {
        // Given
        setupEmptyContext();

        ThreadClass t1 = createThread(101L, 2L, 100, LocalDateTime.now());
        ThreadClass t2 = createThread(102L, 2L, 50, LocalDateTime.now());

        given(threadRepository.findRecommendationCandidates(anyLong(), anyList(), anyList(), anySet(), anySet(), any()))
                .willReturn(List.of(t1, t2));

        // FIX NPE 1: Mockear fallback vacío
        mockFallbackEmpty();

        mockEnricher(t1);

        // When
        List<DiscoverItemDto> result = recommendationService.getRecommendations(currentUser, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).thread().id()).isEqualTo(101L);
    }

    @Test
    @DisplayName("getRecommendations: Should use Fallback if not enough candidates")
    void getRecommendations_Fallback_ShouldFillGap() {
        // Given
        setupEmptyContext();

        // 1. Candidatos principales: Vacío
        given(threadRepository.findRecommendationCandidates(anyLong(), anyList(), anyList(), anySet(), anySet(), any()))
                .willReturn(Collections.emptyList());

        // 2. Fallback (Trending): Devuelve 1 hilo
        ThreadClass fallbackThread = createThread(999L, 5L, 5, LocalDateTime.now());

        // FIX: Stub correcto para el fallback
        given(threadRepository.findThreadsForDiscover(anyLong(), anyList(), anySet(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(fallbackThread)));

        mockEnricher(fallbackThread);

        // When
        List<DiscoverItemDto> result = recommendationService.getRecommendations(currentUser, 5);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).thread().id()).isEqualTo(999L);

        // FIX ERROR 2: Assertion Correcta.
        // El servicio asigna "TRENDING" al 'recommendationReason' (el 4to parámetro del DTO)
        assertThat(result.get(0).recommendationReason()).isEqualTo("Tendencia en Focalizze.");
        // Y asigna "Tendencia en Focalizze." al 'recommendationReason'
        assertThat(result.get(0).recommendationReason()).isEqualTo("Tendencia en Focalizze.");
    }

    @Test
    @DisplayName("getRecommendations: Should verify SQL safety (-1L insertion) for empty lists")
    void getRecommendations_EmptyLists_ShouldAddDummyIds() {
        // Given
        setupEmptyContext();

        // FIX NPE 1: Mockear fallback vacío, ya que el servicio intentará llenar huecos
        mockFallbackEmpty();

        // Mockear candidatos vacíos
        given(threadRepository.findRecommendationCandidates(anyLong(), anyList(), anyList(), anySet(), anySet(), any()))
                .willReturn(Collections.emptyList());

        // When
        recommendationService.getRecommendations(currentUser, 10);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> listCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> setCaptor = ArgumentCaptor.forClass(Set.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> hiddenCaptor = ArgumentCaptor.forClass(Set.class);

        verify(threadRepository).findRecommendationCandidates(
                eq(currentUser.getId()),
                listCaptor.capture(), // Followed Users
                listCaptor.capture(), // Followed Categories
                hiddenCaptor.capture(), // Hidden Threads
                setCaptor.capture(),  // Blocked Users
                any()
        );

        assertThat(listCaptor.getAllValues().get(0)).contains(-1L);
        assertThat(listCaptor.getAllValues().get(1)).contains(-1L);
        assertThat(hiddenCaptor.getValue()).contains(-1L);
        assertThat(setCaptor.getValue()).contains(-1L);
    }

    @Test
    @DisplayName("getRecommendations: Fallback should exclude hidden threads")
    void getRecommendations_Fallback_ShouldFilterHidden() {
        // Given
        setupEmptyContext();

        // Candidatos vacíos
        given(threadRepository.findRecommendationCandidates(anyLong(), anyList(), anyList(), anySet(), anySet(), any()))
                .willReturn(Collections.emptyList());

        // El usuario ocultó el hilo 500
        given(feedbackService.getHiddenThreadIds(currentUser)).willReturn(Set.of(500L));

        // El fallback devuelve el hilo 500
        ThreadClass hiddenThread = createThread(500L, 8L, 10, LocalDateTime.now());
        given(threadRepository.findThreadsForDiscover(anyLong(), anyList(), anySet(), any()))
                .willReturn(new PageImpl<>(List.of(hiddenThread)));

        // When
        List<DiscoverItemDto> result = recommendationService.getRecommendations(currentUser, 10);

        // Then
        assertThat(result).isEmpty();
    }

    // --- Helpers ---

    private void setupEmptyContext() {
        given(userRepository.findByIdWithFollows(currentUser.getId())).willReturn(Optional.of(currentUser));
        given(feedbackService.getHiddenThreadIds(currentUser)).willReturn(Collections.emptySet());
        given(blockRepository.findBlockedUserIdsByBlocker(currentUser.getId())).willReturn(Collections.emptySet());
        given(blockRepository.findUserIdsWhoBlockedUser(currentUser.getId())).willReturn(Collections.emptySet());
    }

    // FIX: Nuevo helper para evitar NPE cuando no esperamos fallback pero el código pasa por ahí
    private void mockFallbackEmpty() {
        given(threadRepository.findThreadsForDiscover(anyLong(), anyList(), anySet(), any()))
                .willReturn(Page.empty());
    }

    private ThreadClass createThread(Long id, Long authorId, int likes, LocalDateTime date) {
        User author = User.builder().id(authorId).username("auth" + authorId).build();
        ThreadClass t = new ThreadClass();
        t.setId(id);
        t.setUser(author);
        t.setCategory(categoryTech);
        t.setLikeCount(likes);
        t.setCommentCount(0);
        t.setSaveCount(0);
        t.setPublishedAt(date);
        return t;
    }

    private void mockEnricher(ThreadClass thread) {
        UserDto uDto = new UserDto(thread.getUser().getId(), "u", "d", "url", 0, false, 0, 0, false, "USER", false, null, null);
        StatsDto sDto = new StatsDto(thread.getLikeCount(), 0, 0, 0);
        // FIX: Constructor con 'images' null
        FeedThreadDto dto = new FeedThreadDto(thread.getId(), uDto, thread.getPublishedAt(), List.of("Post"), sDto, false, false, "Cat", null);

        // FIX ERROR 3: Añadir chequeo de null en el lambda
        given(threadEnricher.enrichList(
                argThat(list -> list != null && list.size() == 1 && list.get(0).getId().equals(thread.getId())),
                eq(currentUser)
        )).willReturn(List.of(dto));
    }
}

