package com.focalizze.Focalizze.serviceTest;

import java.util.Optional;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.servicesImpl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito con JUnit 5
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: Creamos un objeto User de prueba que usaremos en las simulaciones
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@email.com")
                .build();
    }


    @Test
    @DisplayName("Debería devolver un usuario cuando se busca por un nombre de usuario existente")
    void findUserByUserName_WhenUserExists_ShouldReturnUser() {
        // Given: Configuramos el mock del repositorio.
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

        // When: Ejecutamos el método del servicio que queremos probar
        Optional<User> foundUser = userService.findUserByUserName("testuser");

        // Then: Verificamos el resultado
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get()).isEqualTo(testUser);

        // Opcional: Verificar que el método del repositorio fue llamado
        verify(userRepository).findByUsername("testuser");
    }


    @Test
    @DisplayName("Debería devolver un Optional vacío cuando se busca por un nombre de usuario inexistente")
    void findUserByUserName_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Given: "Cuando se llame a userRepository.findByUsername con cualquier String, entonces devuelve un Optional vacío"
        given(userRepository.findByUsername("nonexistentuser")).willReturn(Optional.empty());

        // When
        Optional<User> foundUser = userService.findUserByUserName("nonexistentuser");

        // Then
        assertThat(foundUser).isNotPresent();
    }


    @Test
    @DisplayName("Debería devolver true para un email con formato válido")
    void validateEmail_WhenEmailIsValid_ShouldReturnTrue() {
        // Given
        String validEmail = "test@example.com";

        // When
        boolean result = userService.validateEmail(validEmail);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Debería devolver false para un email con formato inválido")
    void validateEmail_WhenEmailIsInvalid_ShouldReturnFalse() {
        // Given
        String invalidEmail = "esto-no-es-un-email";

        // When
        boolean result = userService.validateEmail(invalidEmail);

        // Then
        assertThat(result).isFalse();
    }


    @Test
    @DisplayName("Debería devolver true si el nombre de usuario está disponible")
    void userNameAvailable_WhenUsernameIsAvailable_ShouldReturnTrue() {
        // Given: "Cuando se llame a findUserNameAvailable con 'newuser', entonces devuelve true"
        given(userRepository.findUserNameAvailable("newuser")).willReturn(true);

        // When
        boolean isAvailable = userService.UserNameAvailable("newuser");

        // Then
        assertThat(isAvailable).isTrue();
    }

    @Test
    @DisplayName("Debería devolver false si el nombre de usuario no está disponible")
    void userNameAvailable_WhenUsernameIsNotAvailable_ShouldReturnFalse() {
        // Given: "Cuando se llame a findUserNameAvailable con 'testuser', entonces devuelve false"
        given(userRepository.findUserNameAvailable("testuser")).willReturn(false);

        // When
        boolean isAvailable = userService.UserNameAvailable("testuser");

        // Then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("Debería devolver un usuario cuando busca un identificador valido")
    void ValidIndentifier_ShouldReturnUser(){
        // --- Caso 1: username ---
        given(userRepository.findByUsernameOrEmail("testuser","testuser")).willReturn(Optional.of(testUser));

        // When: Ejecutamos el método del servicio que queremos probar
        Optional<User> foundUser = userService.findUserByUsernameOrEmail("testuser","testuser");

        // Then: Verificamos el resultado
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get()).isEqualTo(testUser);

        // Opcional: Verificar que el método del repositorio fue llamado
        verify(userRepository).findByUsernameOrEmail("testuser","testuser");


        // --- Caso 2: email ---
        given(userRepository.findByUsernameOrEmail("test@email.com","test@email.com")).willReturn(Optional.of(testUser));

        // When: Ejecutamos el método del servicio que queremos probar
        Optional<User> foundUserEmail = userService.findUserByUsernameOrEmail("test@email.com","test@email.com");

        // Then: Verificamos el resultado
        assertThat(foundUserEmail).isPresent();
        assertThat(foundUserEmail.get()).isEqualTo(testUser);

        // Opcional: Verificar que el método del repositorio fue llamado
        verify(userRepository).findByUsernameOrEmail("test@email.com","test@email.com");

    }
}
