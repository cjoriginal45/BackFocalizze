package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.dto.CategoryDetailsDto;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class CategoryRepositoryTest {
    @Autowired
    private CategoryRepository categoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private CategoryClass testCategory;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: Creamos una categoría base
        testCategory = new CategoryClass();
        testCategory.setName("Technology");
        testCategory.setDescription("All about tech");
        testCategory.setImageUrl("tech.jpg");
        testCategory.setFollowersCount(10);
        entityManager.persist(testCategory);

        // Given: Creamos un usuario para probar el "isFollowing"
        testUser = User.builder()
                .username("tester")
                .email("test@email.com")
                .password("pass")
                .displayName("Tester")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(testUser);

        entityManager.flush(); // Forzamos que se guarden en la BD H2
    }

    @AfterEach
    void tearDown() {
        // Limpiamos el contexto de persistencia para evitar datos en caché entre tests
        entityManager.clear();
    }

    @Test
    @DisplayName("Debería encontrar una categoría por su nombre")
    void findByName_WhenExists_ShouldReturnCategory() {
        // When
        Optional<CategoryClass> found = categoryRepository.findByName("Technology");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testCategory.getId());
    }

    @Test
    @DisplayName("Debería incrementar atómicamente el contador de seguidores")
    void incrementFollowersCount_ShouldIncreaseByOne() {
        // Given: Valor inicial es 10 (seteado en setUp)

        // When
        categoryRepository.incrementFollowersCount(testCategory.getId());

        // Limpiamos para que Hibernate no use la versión vieja en caché
        entityManager.clear();

        // Then
        CategoryClass updatedCategory = categoryRepository.findById(testCategory.getId()).get();
        assertThat(updatedCategory.getFollowersCount()).isEqualTo(11);
    }

    @Test
    @DisplayName("Debería decrementar atómicamente el contador de seguidores")
    void decrementFollowersCount_ShouldDecreaseByOne() {
        // Given: Valor inicial es 10

        // When
        categoryRepository.decrementFollowersCount(testCategory.getId());

        // Limpiamos caché
        entityManager.clear();

        // Then
        CategoryClass updatedCategory = categoryRepository.findById(testCategory.getId()).get();
        assertThat(updatedCategory.getFollowersCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("DTO: Debería traer detalles correctos para usuario GUEST (sin login)")
    void findCategoryDetailsByName_GuestUser_ShouldReturnDetailsWithFalseIsFollowing() {
        // Given
        createThreadForCategory(testCategory, true, false); // Publicado
        createThreadForCategory(testCategory, true, false); // Publicado
        createThreadForCategory(testCategory, false, false); // No publicado
        entityManager.flush();

        // When: currentUserId es NULL
        Optional<CategoryDetailsDto> result = categoryRepository.findCategoryDetailsByName("Technology", null);

        // Then
        assertThat(result).isPresent();
        CategoryDetailsDto dto = result.get();

        assertThat(dto.name()).isEqualTo("Technology");
        assertThat(dto.threadsCount()).isEqualTo(2L); // Solo los 2 publicados
        assertThat(dto.isFollowing()).isFalse();
    }

    @Test
    @DisplayName("DTO: Debería traer detalles correctos para usuario LOGUEADO que SÍ sigue la categoría")
    void findCategoryDetailsByName_LoggedUserFollowing_ShouldReturnTrue() {
        // Given: Creamos la relación de seguimiento manualmente
        CategoryFollow follow = new CategoryFollow();
        follow.setUser(testUser);
        follow.setCategory(testCategory);
        entityManager.persist(follow);
        entityManager.flush();

        // When
        Optional<CategoryDetailsDto> result = categoryRepository.findCategoryDetailsByName("Technology", testUser.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().isFollowing()).isTrue();
        assertThat(result.get().followersCount()).isEqualTo(10);
    }

    // --- Helpers ---

    private void createThreadForCategory(CategoryClass category, boolean isPublished, boolean isDeleted) {
        ThreadClass thread = new ThreadClass();
        thread.setUser(testUser);
        thread.setCategory(category);
        thread.setPublishedAt(LocalDateTime.now());
        thread.setPublished(isPublished);
        thread.setDeleted(isDeleted);

        entityManager.persist(thread);
    }
}
