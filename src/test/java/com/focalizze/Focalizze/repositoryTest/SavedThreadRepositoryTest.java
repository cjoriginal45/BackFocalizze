package com.focalizze.Focalizze.repositoryTest;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
public class SavedThreadRepositoryTest {
    @Autowired
    private SavedThreadRepository savedThreadRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User currentUser;
    private ThreadClass thread1;
    private ThreadClass thread2;
    private ThreadClass thread3;

    @BeforeEach
    void setUp() {
        // 1. Crear Usuario
        currentUser = createUser("saver", "saver@test.com");

        // 2. Crear Categoría (Requerida por Thread)
        CategoryClass category = new CategoryClass();
        category.setName("General");
        category.setDescription("Desc");
        entityManager.persist(category);

        // 3. Crear Hilos (El autor no importa para este test, usaremos el mismo usuario)
        thread1 = createThread(currentUser, category);
        thread2 = createThread(currentUser, category);
        thread3 = createThread(currentUser, category); // Este no lo guardaremos

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("findByUserAndThread: Debería devolver el guardado si existe")
    void findByUserAndThread_WhenExists_ShouldReturnEntity() {
        // Given
        saveThread(currentUser, thread1);
        entityManager.flush();

        // When
        Optional<SavedThreads> result = savedThreadRepository.findByUserAndThread(currentUser, thread1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getThread().getId()).isEqualTo(thread1.getId());
    }

    @Test
    @DisplayName("findByUserAndThread: Debería devolver Empty si no está guardado")
    void findByUserAndThread_WhenNotExists_ShouldReturnEmpty() {
        // Given: No guardamos nada

        // When
        Optional<SavedThreads> result = savedThreadRepository.findByUserAndThread(currentUser, thread1);

        // Then
        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("existsByUserAndThread: Debería retornar true si está guardado")
    void existsByUserAndThread_ShouldReturnTrue() {
        // Given
        saveThread(currentUser, thread2);
        entityManager.flush();

        // When
        boolean exists = savedThreadRepository.existsByUserAndThread(currentUser, thread2);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("findSavedThreadIdsByUserInThreadIds: Debería filtrar y devolver solo los IDs guardados")
    void findSavedThreadIds_ShouldReturnSubset() {
        // Given: Guardamos thread1 y thread2. NO guardamos thread3.
        saveThread(currentUser, thread1);
        saveThread(currentUser, thread2);
        entityManager.flush();

        // Lista de IDs visibles en el feed (incluye el no guardado)
        List<Long> visibleThreadIds = List.of(thread1.getId(), thread2.getId(), thread3.getId());

        // When
        Set<Long> resultIds = savedThreadRepository.findSavedThreadIdsByUserInThreadIds(currentUser, visibleThreadIds);

        // Then
        assertThat(resultIds).hasSize(2);
        assertThat(resultIds).contains(thread1.getId(), thread2.getId());
        assertThat(resultIds).doesNotContain(thread3.getId());
    }

    @Test
    @DisplayName("findByUserOrderByCreatedAtDesc: Debería devolver página ordenada por fecha de guardado")
    void findByUserOrdered_ShouldReturnPageOrdered() {
        // Given
        // Guardamos thread1 primero (Antiguo)
        SavedThreads save1 = SavedThreads.builder()
                .user(currentUser)
                .thread(thread1)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
        entityManager.persist(save1);

        // Guardamos thread2 después (Reciente)
        SavedThreads save2 = SavedThreads.builder()
                .user(currentUser)
                .thread(thread2)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(save2);

        entityManager.flush();
        entityManager.clear(); // Limpiamos para probar el JOIN FETCH

        // When
        Page<SavedThreads> page = savedThreadRepository.findByUserOrderByCreatedAtDesc(currentUser, PageRequest.of(0, 10));

        // Then
        assertThat(page.getContent()).hasSize(2);
        // El más reciente (thread2) debe ir primero
        assertThat(page.getContent().get(0).getThread().getId()).isEqualTo(thread2.getId());
        assertThat(page.getContent().get(1).getThread().getId()).isEqualTo(thread1.getId());

        // Verificación implícita del JOIN FETCH:
        // Si accedemos a getThread().getUser(), no debería fallar ni hacer select extra si el contexto estuviera cerrado.
        assertThat(page.getContent().get(0).getThread().getUser()).isNotNull();
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

    private void saveThread(User user, ThreadClass thread) {
        SavedThreads saved = SavedThreads.builder()
                .user(user)
                .thread(thread)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(saved);
    }
}
