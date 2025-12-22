package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.FollowRepository;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;


import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
public class FollowRepositoryTest {
    @Autowired
    private FollowRepository followRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // 1. Crear usuarios base
        userA = createUser("userA", "a@test.com");
        userB = createUser("userB", "b@test.com");
        userC = createUser("userC", "c@test.com");

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("Debería contar correctamente seguidores y seguidos")
    void countMethods_ShouldReturnCorrectValues() {
        // Given:
        // A sigue a B
        // C sigue a B
        // A sigue a C
        createFollow(userA, userB);
        createFollow(userC, userB);
        createFollow(userA, userC);

        entityManager.flush();

        // When & Then
        // B tiene 2 seguidores (A y C)
        assertThat(followRepository.countByUserFollowed(userB)).isEqualTo(2);

        // A sigue a 2 personas (B y C)
        assertThat(followRepository.countByUserFollower(userA)).isEqualTo(2);

        // C tiene 1 seguidor (A)
        assertThat(followRepository.countByUserFollowed(userC)).isEqualTo(1);
    }

    @Test
    @DisplayName("Debería encontrar una relación de seguimiento específica")
    void findByUserFollowerAndUserFollowed_ShouldReturnOptional() {
        // Given: A sigue a B
        Follow follow = createFollow(userA, userB);
        entityManager.flush();

        // When
        Optional<Follow> found = followRepository.findByUserFollowerAndUserFollowed(userA, userB);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(follow.getId());
    }

    @Test
    @DisplayName("Debería devolver true si existe la relación")
    void existsByUserFollowerAndUserFollowed_ShouldReturnTrue() {
        // Given
        createFollow(userA, userB);
        entityManager.flush();

        // When
        boolean exists = followRepository.existsByUserFollowerAndUserFollowed(userA, userB);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Debería filtrar IDs y devolver solo a los que realmente sigue")
    void findFollowedUserIdsByFollower_ShouldFilterCorrectly() {
        // Given: A sigue a B. A NO sigue a C.
        createFollow(userA, userB);
        entityManager.flush();

        // Lista candidata: IDs de B y C
        Set<Long> candidateIds = Set.of(userB.getId(), userC.getId());

        // When
        Set<Long> resultIds = followRepository.findFollowedUserIdsByFollower(userA, candidateIds);

        // Then: Solo debería devolver el ID de B
        assertThat(resultIds).hasSize(1);
        assertThat(resultIds).contains(userB.getId());
        assertThat(resultIds).doesNotContain(userC.getId());
    }

    @Test
    @DisplayName("Debería eliminar una relación de seguimiento y devolver 1")
    void deleteFollowRelation_ShouldRemoveAndReturnCount() {
        // Given: A sigue a B
        createFollow(userA, userB);
        entityManager.flush();

        // When
        int deletedCount = followRepository.deleteFollowRelation(userA, userB);

        // Limpiamos caché para asegurar que la siguiente consulta vaya a la BD
        entityManager.clear();

        // Then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(followRepository.existsByUserFollowerAndUserFollowed(userA, userB)).isFalse();
    }

    @Test
    @DisplayName("Debería listar los seguidores de un usuario por username (ordenados por fecha)")
    void findFollowersByUsername_ShouldReturnOrderedList() {
        // Given: B es el objetivo
        // 1. A sigue a B (Antiguo)
        Follow f1 = createFollow(userA, userB);
        f1.setCreatedAt(LocalDateTime.now().minusDays(5));
        entityManager.persist(f1);

        // 2. C sigue a B (Reciente)
        Follow f2 = createFollow(userC, userB);
        f2.setCreatedAt(LocalDateTime.now());
        entityManager.persist(f2);

        entityManager.flush();

        // When
        List<User> followers = followRepository.findFollowersByUsername(userB.getUsername());

        // Then: Debería devolver a C (reciente) y luego a A (antiguo)
        assertThat(followers).hasSize(2);
        assertThat(followers.get(0).getUsername()).isEqualTo(userC.getUsername());
        assertThat(followers.get(1).getUsername()).isEqualTo(userA.getUsername());
    }

    @Test
    @DisplayName("Debería listar a quién sigue un usuario por username (ordenados por fecha)")
    void findFollowingByUsername_ShouldReturnOrderedList() {
        // Given: A es el que sigue
        // 1. A sigue a B (Antiguo)
        Follow f1 = createFollow(userA, userB);
        f1.setCreatedAt(LocalDateTime.now().minusDays(5));
        entityManager.persist(f1);

        // 2. A sigue a C (Reciente)
        Follow f2 = createFollow(userA, userC);
        f2.setCreatedAt(LocalDateTime.now());
        entityManager.persist(f2);

        entityManager.flush();

        // When
        List<User> following = followRepository.findFollowingByUsername(userA.getUsername());

        // Then: Debería devolver a C (reciente) y luego a B (antiguo)
        assertThat(following).hasSize(2);
        assertThat(following.get(0).getUsername()).isEqualTo(userC.getUsername());
        assertThat(following.get(1).getUsername()).isEqualTo(userB.getUsername());
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

    private Follow createFollow(User follower, User followed) {
        Follow follow = Follow.builder()
                .userFollower(follower)
                .userFollowed(followed)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(follow);
        return follow;
    }
}
