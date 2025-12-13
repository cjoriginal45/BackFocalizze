package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: Usuario base para pruebas generales
        testUser = User.builder()
                .username("testuser")
                .email("test@email.com")
                .password("pass123")
                .displayName("Test User")
                .role(UserRole.USER)
                .followingCount(0)
                .followersCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
        // No es estrictamente necesario con @Transactional, pero buena práctica si se cambia la configuración
    }

    // --- TESTS BÁSICOS ---

    @Test
    @DisplayName("findByUsername: Debería encontrar usuario si existe")
    void findByUsername_WhenExists_ShouldReturnUser() {
        Optional<User> found = userRepository.findByUsername("testuser");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findByEmail: Debería encontrar usuario si existe")
    void findByEmail_WhenExists_ShouldReturnUser() {
        Optional<User> found = userRepository.findByEmail("test@email.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("findByUsernameOrEmail: Debería encontrar por cualquiera de los dos")
    void findByUsernameOrEmail_ShouldReturnUser() {
        // Por username
        assertThat(userRepository.findByUsernameOrEmail("testuser", "wrong@mail.com")).isPresent();
        // Por email
        assertThat(userRepository.findByUsernameOrEmail("wrong", "test@email.com")).isPresent();
    }

    @Test
    @DisplayName("findUserNameAvailable: Debería devolver false si ya existe")
    void findUserNameAvailable_WhenExists_ShouldReturnFalse() {
        boolean available = userRepository.findUserNameAvailable("testuser");
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("findUserNameAvailable: Debería devolver true si no existe")
    void findUserNameAvailable_WhenNotExists_ShouldReturnTrue() {
        boolean available = userRepository.findUserNameAvailable("newuser123");
        assertThat(available).isTrue();
    }

    // --- AUTOCOMPLETADO ---

    @Test
    @DisplayName("findTop5ByUsername...: Debería devolver usuarios que coincidan con el prefijo")
    void autocomplete_ShouldReturnMatchingUsers() {
        // Given
        createExtraUser("tester1");
        createExtraUser("tester2");
        createExtraUser("other"); // No coincide
        entityManager.flush();

        // When
        List<User> results = userRepository.findTop5ByUsernameStartingWithIgnoreCase("test");

        // Then
        assertThat(results).hasSize(3); // testuser + tester1 + tester2
        assertThat(results).extracting(User::getUsername).contains("testuser", "tester1", "tester2");
    }

    @Test
    @DisplayName("findTop5...AndIdNotIn: Debería excluir IDs especificados")
    void autocomplete_ShouldExcludeIds() {
        // Given
        User u1 = createExtraUser("apple");
        User u2 = createExtraUser("april");
        entityManager.flush();

        // When: Buscamos "ap" pero excluimos a u1
        List<User> results = userRepository.findTop5ByUsernameStartingWithIgnoreCaseAndIdNotIn("ap", Set.of(u1.getId()));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("april");
    }

    // --- CONTADORES (Atomic Updates) ---

    @Test
    @DisplayName("incrementFollowingCount: Debería aumentar el contador en 1")
    void incrementFollowingCount_ShouldIncrease() {
        // When
        userRepository.incrementFollowingCount(testUser.getId());
        entityManager.clear(); // Limpiar caché para ver cambio en BD

        // Then
        User updated = userRepository.findById(testUser.getId()).get();
        assertThat(updated.getFollowingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("decrementFollowingCount: Debería disminuir el contador en 1")
    void decrementFollowingCount_ShouldDecrease() {
        // Given
        testUser.setFollowingCount(5);
        entityManager.merge(testUser);
        entityManager.flush();

        // When
        userRepository.decrementFollowingCount(testUser.getId());
        entityManager.clear();

        // Then
        User updated = userRepository.findById(testUser.getId()).get();
        assertThat(updated.getFollowingCount()).isEqualTo(4);
    }

    // (Lo mismo aplica para incrementFollowersCount y decrementFollowersCount, funcionan igual)

    // --- LISTAS Y TOKENS ---

    @Test
    @DisplayName("findAllByUsernameIn: Debería encontrar múltiples usuarios")
    void findAllByUsernameIn_ShouldReturnList() {
        // Given
        createExtraUser("u1");
        createExtraUser("u2");
        entityManager.flush();

        // When
        List<User> users = userRepository.findAllByUsernameIn(List.of("testuser", "u1"));

        // Then
        assertThat(users).hasSize(2);
    }

    @Test
    @DisplayName("findByResetPasswordToken: Debería encontrar usuario por token")
    void findByResetPasswordToken_ShouldReturnUser() {
        // Given
        testUser.setResetPasswordToken("token123");
        entityManager.merge(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByResetPasswordToken("token123");

        // Then
        assertThat(found).isPresent();
    }

    // --- JOIN FETCH ---

    @Test
    @DisplayName("findByIdWithFollows: Debería cargar usuario y sus relaciones")
    void findByIdWithFollows_ShouldFetchData() {
        // Given: Agregamos una relación de follow y una categoría
        User followed = createExtraUser("followed");

        Follow follow = new Follow();
        follow.setUserFollower(testUser);
        follow.setUserFollowed(followed);
        follow.setCreatedAt(LocalDateTime.now());
        entityManager.persist(follow);

        CategoryClass cat = new CategoryClass();
        cat.setName("Tech");
        entityManager.persist(cat);

        CategoryFollow catFollow = new CategoryFollow();
        catFollow.setUser(testUser);
        catFollow.setCategory(cat);
        entityManager.persist(catFollow);

        entityManager.flush();
        entityManager.clear();

        // When
        Optional<User> result = userRepository.findByIdWithFollows(testUser.getId());

        // Then
        assertThat(result).isPresent();
        // Accedemos a las listas para verificar que no explote (LazyInitialization) y tenga datos
        assertThat(result.get().getFollowing()).hasSize(1);
        assertThat(result.get().getFollowedCategories()).hasSize(1);
    }

    // --- BLOQUEOS ---

    @Test
    @DisplayName("findBlockedUserIdsByBlocker: Debería devolver a quién ha bloqueado el usuario")
    void findBlockedUserIdsByBlocker_ShouldReturnIds() {
        // Given: testUser bloquea a 'blockedUser'
        User blockedUser = createExtraUser("blocked");

        Block block = Block.builder()
                .blocker(testUser)
                .blocked(blockedUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(block);
        entityManager.flush();

        // When
        Set<Long> ids = userRepository.findBlockedUserIdsByBlocker(testUser.getId());

        // Then
        assertThat(ids).containsExactly(blockedUser.getId());
    }

    @Test
    @DisplayName("findUserIdsWhoBlockedUser: Debería devolver quién ha bloqueado al usuario")
    void findUserIdsWhoBlockedUser_ShouldReturnIds() {
        // Given: 'blockerUser' bloquea a testUser
        User blockerUser = createExtraUser("blocker");

        Block block = Block.builder()
                .blocker(blockerUser)
                .blocked(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(block);
        entityManager.flush();

        // When
        Set<Long> ids = userRepository.findUserIdsWhoBlockedUser(testUser.getId());

        // Then
        assertThat(ids).containsExactly(blockerUser.getId());
    }

    // --- Helpers ---
    private User createExtraUser(String username) {
        User u = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password("pass")
                .displayName(username)
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(u);
        return u;
    }
}
