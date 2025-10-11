package com.focalizze.Focalizze.repositoryTest;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.models.UserRole;
import com.focalizze.Focalizze.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class UserRepositoryTest {
    @Autowired // Spring inyectará la implementación del repositorio en este entorno de test
    private UserRepository userRepository;

    private User testUser;

    // Este método se ejecuta antes de cada test
    @BeforeEach
    void setUp() {
        // Given: Creamos un usuario de prueba para tener datos consistentes
        testUser = User.builder()
                .username("testuser")
                .email("test@email.com")
                .password("hashedpassword")
                .displayName("Test User")
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);
    }

    // Este método se ejecuta después de cada test para limpiar la BD
    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }


    @Test
    @DisplayName("Debería encontrar un usuario por su nombre de usuario si existe")
    void findByUsername_WhenUserExists_ShouldReturnUser() {
        // When: Ejecutamos el método del repositorio que queremos probar
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Then: Verificamos que los resultados son los esperados
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("No debería encontrar un usuario por su nombre de usuario si no existe")
    void findByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // When: Buscamos un usuario que sabemos que no existe
        Optional<User> foundUser = userRepository.findByUsername("nonexistentuser");

        // Then: Verificamos que el Optional está vacío
        assertThat(foundUser).isNotPresent();
    }


    @Test
    @DisplayName("Debería encontrar un usuario por su email si existe")
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        // When: Ejecutamos el método del repositorio
        Optional<User> foundUser = userRepository.findByEmail("test@email.com");

        // Then: Verificamos los resultados
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }


    @Test
    @DisplayName("Debería encontrar un usuario por username O email")
    void findByUsernameOrEmail_WhenEitherMatches_ShouldReturnUser() {
        // --- Caso 1: Coincide el username ---
        // When
        Optional<User> foundByUsername = userRepository.findByUsernameOrEmail("testuser", "wrong@email.com");
        // Then
        assertThat(foundByUsername).isPresent();
        assertThat(foundByUsername.get().getId()).isEqualTo(testUser.getId());

        // --- Caso 2: Coincide el email ---
        // When
        Optional<User> foundByEmail = userRepository.findByUsernameOrEmail("wronguser", "test@email.com");
        // Then
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("No debería encontrar un usuario si ni el username ni el email coinciden")
    void findByUsernameOrEmail_WhenNeitherMatches_ShouldReturnEmpty() {
        // When
        Optional<User> notFoundUser = userRepository.findByUsernameOrEmail("wronguser", "wrong@email.com");

        // Then
        assertThat(notFoundUser).isNotPresent();
    }


    @Test
    @DisplayName("Debería devolver false si el nombre de usuario ya está en uso")
    void findUserNameAvailable_WhenUsernameExists_ShouldReturnFalse() {
        // When: Verificamos la disponibilidad de un username que ya existe
        boolean isAvailable = userRepository.findUserNameAvailable("testuser");

        // Then: Verificamos que el resultado es false (no está disponible)
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("Debería devolver true si el nombre de usuario está disponible")
    void findUserNameAvailable_WhenUsernameDoesNotExist_ShouldReturnTrue() {
        // When: Verificamos la disponibilidad de un username que no existe
        boolean isAvailable = userRepository.findUserNameAvailable("newuser");

        // Then: Verificamos que el resultado es true (está disponible)
        assertThat(isAvailable).isTrue();
    }
}
