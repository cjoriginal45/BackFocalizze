package com.focalizze.Focalizze.repositoryTest;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.LikeRepository;
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
public class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User liker;
    private ThreadClass thread;

    @BeforeEach
    void setUp() {
        // 1. Usuario que da like
        liker = createUser("liker", "liker@test.com");

        // 2. Autor del hilo (necesario para crear el hilo)
        User author = createUser("author", "author@test.com");

        // 3. Categoría (necesaria para crear el hilo)
        CategoryClass category = new CategoryClass();
        category.setName("General");
        category.setDescription("Desc");
        entityManager.persist(category);

        // 4. Hilo
        thread = new ThreadClass();
        thread.setUser(author);
        thread.setCategory(category);
        thread.setPublishedAt(LocalDateTime.now());
        thread.setPublished(true);
        thread.setDeleted(false);
        entityManager.persist(thread);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("findByUserAndThread: Debería encontrar el Like si existe")
    void findByUserAndThread_WhenExists_ShouldReturnLike() {
        // Given
        Like like = Like.builder()
                .user(liker)
                .thread(thread)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(like);
        entityManager.flush();

        // When
        Optional<Like> result = likeRepository.findByUserAndThread(liker, thread);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isEqualTo(liker);
        assertThat(result.get().getThread()).isEqualTo(thread);
    }

    @Test
    @DisplayName("findByUserAndThread: Debería devolver Empty si no existe")
    void findByUserAndThread_WhenNotExists_ShouldReturnEmpty() {
        // Given: No creamos ningún Like

        // When
        Optional<Like> result = likeRepository.findByUserAndThread(liker, thread);

        // Then
        assertThat(result).isNotPresent();
    }

    // --- Helpers ---
    private User createUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("pass")
                .displayName(username)
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(user);
        return user;
    }
}
