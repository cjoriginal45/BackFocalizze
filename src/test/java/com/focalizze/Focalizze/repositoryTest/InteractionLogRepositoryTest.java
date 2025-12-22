package com.focalizze.Focalizze.repositoryTest;
import com.focalizze.Focalizze.models.InteractionLog;
import com.focalizze.Focalizze.models.InteractionType;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.InteractionLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
public class InteractionLogRepositoryTest {
    @Autowired
    private InteractionLogRepository interactionLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("interactor")
                .email("act@test.com")
                .password("pass")
                .displayName("Interactor")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("countByUserAndCreatedAtAfter: Debería contar interacciones dentro del rango de tiempo")
    void countByUser_ShouldCountCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // Interacción reciente (dentro del rango)
        createLog(testUser, InteractionType.LIKE, now);
        // Interacción reciente (dentro del rango)
        createLog(testUser, InteractionType.COMMENT, now.minusMinutes(30));
        // Interacción antigua (fuera del rango)
        createLog(testUser, InteractionType.LIKE, now.minusHours(25));

        entityManager.flush();

        // When: Contamos desde hace 24 horas
        LocalDateTime startOfDay = now.minusHours(24);
        long count = interactionLogRepository.countByUserAndCreatedAtAfter(testUser, startOfDay);

        // Then: Debería ser 2
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("findFirst...: Debería encontrar la interacción más reciente de un tipo específico")
    void findFirst_ShouldReturnLatestLog() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // Log antiguo
        createLog(testUser, InteractionType.LIKE, now.minusHours(2));
        // Log reciente (el que esperamos)
        InteractionLog recentLog = createLog(testUser, InteractionType.LIKE, now.minusMinutes(5));

        entityManager.flush();

        // When
        Optional<InteractionLog> result = interactionLogRepository
                .findFirstByUserAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                        testUser, InteractionType.LIKE, now.minusHours(24));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(recentLog.getId());
    }

    @Test
    @DisplayName("findLogsToRefund: Debería devolver logs filtrados por tipo, usuario y fecha, ordenados DESC")
    void findLogsToRefund_ShouldFilterAndSort() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.minusHours(10);

        // 1. LIKE Reciente (Esperado 1º)
        InteractionLog log1 = createLog(testUser, InteractionType.LIKE, now);
        // 2. LIKE Un poco menos reciente (Esperado 2º)
        InteractionLog log2 = createLog(testUser, InteractionType.LIKE, now.minusMinutes(5));
        // 3. COMMENT Reciente (No debe salir, mal tipo)
        createLog(testUser, InteractionType.COMMENT, now);
        // 4. LIKE Antiguo (No debe salir, fecha anterior a startOfDay)
        createLog(testUser, InteractionType.LIKE, now.minusHours(12));

        entityManager.flush();

        // When
        List<InteractionLog> logs = interactionLogRepository.findLogsToRefund(testUser, InteractionType.LIKE, startOfDay);

        // Then
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getId()).isEqualTo(log1.getId()); // El más reciente primero
        assertThat(logs.get(1).getId()).isEqualTo(log2.getId());
    }

    private InteractionLog createLog(User user, InteractionType type, LocalDateTime createdAt) {
        InteractionLog log = InteractionLog.builder()
                .user(user)
                .type(type)
                .createdAt(createdAt)
                .build();
        entityManager.persist(log);
        return log;
    }
}
