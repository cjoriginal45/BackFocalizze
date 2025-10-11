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
    @Autowired
    private UserRepository userRepository;

    private User testUser;

    // Este método se ejecuta antes de cada test
    // This method is executed before each test
    @BeforeEach
    void setUp() {
        // Given: Creamos un usuario de prueba para tener datos consistentes
        // Given: We create a test user to have consistent data
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
    // This method is executed after each test to clean the DB
    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }


    @Test
    @DisplayName("Debería encontrar un usuario por su nombre de usuario si existe")
    void findByUsername_WhenUserExists_ShouldReturnUser() {
        // When: Ejecutamos el método del repositorio que queremos probar
        // When: We execute the method of the repository that we want to test
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Then: Verificamos que los resultados son los esperados
        // Then: We verify that the results are as expected
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("No debería encontrar un usuario por su nombre de usuario si no existe")
    void findByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // When: Buscamos un usuario que sabemos que no existe
        // When: We are looking for a user that we know does not exist
        Optional<User> foundUser = userRepository.findByUsername("nonexistentuser");

        // Then: Verificamos que el Optional está vacío
        // Then: We verify that the Optional is empty
        assertThat(foundUser).isNotPresent();
    }


    @Test
    @DisplayName("Debería encontrar un usuario por su email si existe")
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        // When: Ejecutamos el método del repositorio
        // When: We execute the repository method
        Optional<User> foundUser = userRepository.findByEmail("test@email.com");

        // Then: Verificamos los resultados
        // Then: We check the results
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }


    @Test
    @DisplayName("Debería encontrar un usuario por username O email")
    void findByUsernameOrEmail_WhenEitherMatches_ShouldReturnUser() {
        // --- Caso 1: Coincide el username ---
        // --- Case 1: Username matches ---
        // When
        Optional<User> foundByUsername = userRepository.findByUsernameOrEmail("testuser", "wrong@email.com");
        // Then
        assertThat(foundByUsername).isPresent();
        assertThat(foundByUsername.get().getId()).isEqualTo(testUser.getId());

        // --- Caso 2: Coincide el email ---
        // --- Case 2: Email matches ---
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
        // When: We check the availability of a username that already exists
        boolean isAvailable = userRepository.findUserNameAvailable("testuser");

        // Then: Verificamos que el resultado es false (no está disponible)
        // Then: We verify that the result is false (not available)
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("Debería devolver true si el nombre de usuario está disponible")
    void findUserNameAvailable_WhenUsernameDoesNotExist_ShouldReturnTrue() {
        // When: Verificamos la disponibilidad de un username que no existe
        // When: We check the availability of a username that does not exist
        boolean isAvailable = userRepository.findUserNameAvailable("newuser");

        // Then: Verificamos que el resultado es true (está disponible)
        // Then: We verify that the result is true (it is available)
        assertThat(isAvailable).isTrue();
    }
}
