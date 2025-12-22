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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "jwt.secret.key=una_clave_secreta_muy_larga_para_tests_que_sea_segura_123456",
        "file.upload-dir=uploads-test",
        "app.default-avatar-url=http://localhost:8080/images/default-avatar.png"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // Definimos constantes para evitar errores de tipeo (typos)
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // 1. Limpiar BD
        userRepository.deleteAll();

        // 2. Crear usuario
        User user = User.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL) // Usamos la constante
                .password("password123")
                .displayName("Test User")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .isTwoFactorEnabled(false)
                .tokenVersion(0)
                .backgroundType("default")
                .dailyThreadsRemaining(5)
                .dailyInteractionsRemaining(50)
                .followersCount(0)
                .followingCount(0)
                .build();

        userRepository.save(user);
    }


    // --- TESTS BÁSICOS ---

    @Test
    @DisplayName("Debería encontrar usuario por username")
    void findByUsername_WhenExists_ShouldReturnUser() {
        Optional<User> foundUser = userRepository.findByUsername(TEST_USERNAME);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("No debería encontrar usuario si el username no existe")
    void findByUsername_WhenNotExists_ShouldReturnEmpty() {
        Optional<User> foundUser = userRepository.findByUsername("usuario_fantasma");

        assertThat(foundUser).isNotPresent();
    }

    @Test
    @DisplayName("Debería encontrar usuario por email")
    void findByEmail_WhenExists_ShouldReturnUser() {
        // Este es el test que fallaba. Ahora usamos la constante TEST_EMAIL
        // para asegurar que coincida con lo guardado en setUp().
        Optional<User> foundUser = userRepository.findByEmail(TEST_EMAIL);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Debería verificar si el username está disponible")
    void findUserNameAvailable_True() {
        // Buscamos un usuario que NO existe ("newuser")
        boolean isAvailable = userRepository.findUserNameAvailable("newuser");

        // Debería estar disponible (true)
        assertThat(isAvailable).isTrue();
    }

    @Test
    @DisplayName("Debería verificar si el username NO está disponible")
    void findUserNameAvailable_False() {
        // Buscamos el usuario que SÍ existe ("testuser")
        boolean isAvailable = userRepository.findUserNameAvailable(TEST_USERNAME);

        // No debería estar disponible (false)
        assertThat(isAvailable).isFalse();
    }
}
