package com.focalizze.Focalizze.serviceTest;
import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.dto.CategoryDto;
import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.CategoryFollow;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.CategoryServiceImpl;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadEnricher threadEnricher;
    @Mock private BlockRepository blockRepository;

    // Mocks de Seguridad
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User currentUser;
    private CategoryClass category1;
    private CategoryClass category2;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        currentUser = User.builder().id(1L).username("user").followedCategories(new HashSet<>()).build();

        category1 = new CategoryClass(); category1.setId(10L); category1.setName("Tech");
        category2 = new CategoryClass(); category2.setId(20L); category2.setName("Art");
    }

    // --- getAllCategories ---

    @Test
    @DisplayName("getAllCategories: Should identify followed categories if user is logged in")
    void getAllCategories_LoggedIn_ShouldMarkFollowed() {
        // Given
        CategoryFollow cf = CategoryFollow.builder().category(category1).user(currentUser).build();
        currentUser.getFollowedCategories().add(cf);

        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);
        given(userRepository.findById(1L)).willReturn(Optional.of(currentUser));
        given(categoryRepository.findAll()).willReturn(List.of(category1, category2));

        // When
        List<CategoryDto> result = categoryService.getAllCategories();

        // Then
        assertThat(result).hasSize(2);

        // Category 1 (Tech) debería tener isFollowing = true
        assertThat(result.stream().filter(c -> c.id().equals(10L)).findFirst().get().isFollowedByCurrentUser()).isTrue();

        // Category 2 (Art) debería tener isFollowing = false
        assertThat(result.stream().filter(c -> c.id().equals(20L)).findFirst().get().isFollowedByCurrentUser()).isFalse();
    }

    @Test
    @DisplayName("getAllCategories: Should return all false if user is not logged in")
    void getAllCategories_Guest_ShouldReturnAllFalse() {
        // Given: No authentication
        given(securityContext.getAuthentication()).willReturn(null);
        given(categoryRepository.findAll()).willReturn(List.of(category1));

        // When
        List<CategoryDto> result = categoryService.getAllCategories();

        // Then
        assertThat(result.get(0).isFollowedByCurrentUser()).isFalse();
    }

    // --- getCategoryDetails ---

    @Test
    @DisplayName("getCategoryDetails: Should return DTO if found")
    void getCategoryDetails_Found_ReturnsDto() {
        // Given
        CategoryDetailsDto dto = new CategoryDetailsDto(10L, "Tech", "Desc", null, 0, 0L, false);
        given(categoryRepository.findCategoryDetailsByName(eq("Tech"), any())).willReturn(Optional.of(dto));

        // When
        CategoryDetailsDto result = categoryService.getCategoryDetails("Tech");

        // Then
        assertThat(result.name()).isEqualTo("Tech");
    }

    @Test
    @DisplayName("getCategoryDetails: Should throw exception if not found")
    void getCategoryDetails_NotFound_ThrowsException() {
        // Given
        given(categoryRepository.findCategoryDetailsByName(eq("Unknown"), any())).willReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> categoryService.getCategoryDetails("Unknown"));
    }

    // --- getThreadsByCategory (Lógica de Bloqueo) ---

    @Test
    @DisplayName("getThreadsByCategory: Should filter out threads from blocked users")
    void getThreadsByCategory_WithBlocks_ShouldFilter() {
        // Given
        User blockedUser = User.builder().id(99L).username("blocked").build();
        User cleanUser = User.builder().id(50L).username("clean").build();

        ThreadClass threadBlocked = new ThreadClass(); threadBlocked.setId(100L); threadBlocked.setUser(blockedUser);
        ThreadClass threadClean = new ThreadClass(); threadClean.setId(101L); threadClean.setUser(cleanUser);

        Page<ThreadClass> rawPage = new PageImpl<>(List.of(threadBlocked, threadClean));

        // Mocks
        given(threadRepository.findPublishedThreadsByCategoryName(eq("Tech"), any(Pageable.class))).willReturn(rawPage);

        // Simular Login
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(currentUser);

        // Simular Bloqueos
        given(blockRepository.findBlockedUserIdsByBlocker(currentUser.getId())).willReturn(Set.of(99L));
        given(blockRepository.findUserIdsWhoBlockedUser(currentUser.getId())).willReturn(Collections.emptySet());

        // When
        categoryService.getThreadsByCategory("Tech", Pageable.unpaged());

        // Then
        // Verificamos que al enriquecedor SOLO se le pasó el hilo limpio (threadClean)
        // threadBlocked debió ser filtrado en memoria
        verify(threadEnricher).enrichList(
                argThat(list -> list.size() == 1 && list.get(0).getId().equals(101L)),
                eq(currentUser)
        );
    }
}
