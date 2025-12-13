package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.CategoryFollow;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.CategoryFollowRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class CategoryFollowRepositoryTest {
    @Autowired
    private CategoryFollowRepository categoryFollowRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User testUser;
    private CategoryClass testCategory;

    @BeforeEach
    void setUp() {
        // 1. Crear y persistir un usuario
        testUser = User.builder()
                .username("catFollower")
                .email("follower@cat.com")
                .password("pass")
                .displayName("Category Follower")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(testUser);

        // 2. Crear y persistir una categoría
        testCategory = new CategoryClass();
        testCategory.setName("Programming");
        testCategory.setDescription("Code stuff");
        testCategory.setImageUrl("code.png");
        testCategory.setFollowersCount(0);
        entityManager.persist(testCategory);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("Debería encontrar la relación si el usuario sigue la categoría")
    void findByUserAndCategory_WhenFollowExists_ShouldReturnFollow() {
        // Given: Creamos la relación de seguimiento
        CategoryFollow follow = new CategoryFollow();
        follow.setUser(testUser);
        follow.setCategory(testCategory);

        entityManager.persist(follow);
        entityManager.flush();

        // When: Buscamos en el repositorio
        Optional<CategoryFollow> result = categoryFollowRepository.findByUserAndCategory(testUser, testCategory);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(result.get().getCategory().getId()).isEqualTo(testCategory.getId());
    }

    @Test
    @DisplayName("Debería devolver Empty si el usuario NO sigue la categoría")
    void findByUserAndCategory_WhenNotFollowing_ShouldReturnEmpty() {
        // Given: No creamos ninguna relación CategoryFollow

        // When
        Optional<CategoryFollow> result = categoryFollowRepository.findByUserAndCategory(testUser, testCategory);

        // Then
        assertThat(result).isNotPresent();
    }
}
