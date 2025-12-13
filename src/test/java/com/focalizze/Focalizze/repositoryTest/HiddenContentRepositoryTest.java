package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.HiddenContentRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class HiddenContentRepositoryTest {
    @Autowired
    private HiddenContentRepository hiddenContentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User currentUser;
    private User otherUser;
    private ThreadClass thread1;
    private ThreadClass thread2;

    @BeforeEach
    void setUp() {
        // 1. Crear usuarios
        currentUser = createUser("currentUser", "curr@test.com");
        otherUser = createUser("otherUser", "other@test.com");

        // 2. Crear una categoría (necesaria para crear hilos)
        CategoryClass category = new CategoryClass();
        category.setName("General");
        category.setDescription("General stuff");
        entityManager.persist(category);

        // 3. Crear hilos (autores indistintos para este test)
        thread1 = createThread(otherUser, category);
        thread2 = createThread(otherUser, category);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("Debería devolver los IDs de los hilos ocultos por el usuario")
    void findHiddenThreadIdsByUser_ShouldReturnIds() {
        // Given: currentUser oculta el thread1
        HiddenContent hidden = new HiddenContent();
        hidden.setUser(currentUser);
        hidden.setThread(thread1);
        entityManager.persist(hidden);

        entityManager.flush();

        // When
        Set<Long> hiddenIds = hiddenContentRepository.findHiddenThreadIdsByUser(currentUser);

        // Then
        assertThat(hiddenIds).hasSize(1);
        assertThat(hiddenIds).contains(thread1.getId());
        assertThat(hiddenIds).doesNotContain(thread2.getId());
    }

    @Test
    @DisplayName("No debería devolver hilos ocultos por OTROS usuarios")
    void findHiddenThreadIdsByUser_ShouldNotReturnOtherUsersHiddenContent() {
        // Given: otherUser oculta el thread2. currentUser no oculta nada.
        HiddenContent hiddenByOther = new HiddenContent();
        hiddenByOther.setUser(otherUser);
        hiddenByOther.setThread(thread2);
        entityManager.persist(hiddenByOther);

        entityManager.flush();

        // When: Buscamos los ocultos de currentUser
        Set<Long> hiddenIds = hiddenContentRepository.findHiddenThreadIdsByUser(currentUser);

        // Then: Debería estar vacío
        assertThat(hiddenIds).isEmpty();
    }

    @Test
    @DisplayName("Debería devolver múltiples IDs si el usuario oculta varios hilos")
    void findHiddenThreadIdsByUser_MultipleThreads_ShouldReturnAll() {
        // Given: currentUser oculta thread1 Y thread2
        HiddenContent h1 = new HiddenContent();
        h1.setUser(currentUser);
        h1.setThread(thread1);
        entityManager.persist(h1);

        HiddenContent h2 = new HiddenContent();
        h2.setUser(currentUser);
        h2.setThread(thread2);
        entityManager.persist(h2);

        entityManager.flush();

        // When
        Set<Long> hiddenIds = hiddenContentRepository.findHiddenThreadIdsByUser(currentUser);

        // Then
        assertThat(hiddenIds).hasSize(2);
        assertThat(hiddenIds).contains(thread1.getId(), thread2.getId());
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

    private ThreadClass createThread(User author, CategoryClass category) {
        ThreadClass thread = new ThreadClass();
        thread.setUser(author);
        thread.setCategory(category);
        thread.setPublishedAt(LocalDateTime.now());
        thread.setPublished(true);
        thread.setDeleted(false);
        entityManager.persist(thread);
        return thread;
    }
}
