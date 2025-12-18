package com.focalizze.Focalizze.serviceTest;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.CategoryFollow;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryFollowRepository;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.services.servicesImpl.CategoryFollowServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CategoryFollowServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryFollowRepository categoryFollowRepository;

    @InjectMocks
    private CategoryFollowServiceImpl categoryFollowService;

    private User currentUser;
    private CategoryClass category;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("user").build();
        category = new CategoryClass();
        category.setId(100L);
        category.setFollowersCount(5);
    }

    @Test
    @DisplayName("toggleFollowCategory: Should increment followers and save if not following")
    void toggle_WhenNotFollowing_ShouldFollow() {
        // Given
        given(categoryRepository.findById(100L)).willReturn(Optional.of(category));
        given(categoryFollowRepository.findByUserAndCategory(currentUser, category)).willReturn(Optional.empty());

        // When
        categoryFollowService.toggleFollowCategory(100L, currentUser);

        // Then
        verify(categoryFollowRepository).save(any(CategoryFollow.class));
        verify(categoryRepository).incrementFollowersCount(100L);
    }

    @Test
    @DisplayName("toggleFollowCategory: Should decrement followers and delete if already following")
    void toggle_WhenFollowing_ShouldUnfollow() {
        // Given
        CategoryFollow existingFollow = CategoryFollow.builder().user(currentUser).category(category).build();

        given(categoryRepository.findById(100L)).willReturn(Optional.of(category));
        given(categoryFollowRepository.findByUserAndCategory(currentUser, category)).willReturn(Optional.of(existingFollow));

        // When
        categoryFollowService.toggleFollowCategory(100L, currentUser);

        // Then
        verify(categoryFollowRepository).delete(existingFollow);
        verify(categoryRepository).decrementFollowersCount(100L);
    }

    @Test
    @DisplayName("toggleFollowCategory: Should throw exception if category not found")
    void toggle_CategoryNotFound_ThrowsException() {
        // Given
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () ->
                categoryFollowService.toggleFollowCategory(999L, currentUser)
        );
    }
}
