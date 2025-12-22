package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.UpdateThemeDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link UserService} interface.
 * Handles user retrieval, profile management, and validation logic.
 * <p>
 * Implementación de la interfaz {@link UserService}.
 * Maneja la recuperación de usuarios, gestión de perfiles y lógica de validación.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;

    // Compile regex once to improve performance (Static Pattern)
    // Compilar regex una vez para mejorar el rendimiento (Patrón Estático)
    private static final String EMAIL_REGEX = "^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:(?!-)[A-Za-z0-9-]+(?<!-)\\.)+[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * Finds a user by their username.
     * <p>
     * Encuentra un usuario por su nombre de usuario.
     *
     * @param username The username. / El nombre de usuario.
     * @return Optional User.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByUserName(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Finds a user by their email address.
     * <p>
     * Encuentra un usuario por su dirección de correo electrónico.
     *
     * @param email The email address. / La dirección de correo.
     * @return Optional User.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Finds a user by username or email.
     * <p>
     * Encuentra un usuario por nombre de usuario o correo.
     *
     * @param username The username. / El nombre de usuario.
     * @param email    The email. / El correo.
     * @return Optional User.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameOrEmail(username,email);
    }

    /**
     * Validates an email format using a strict regex pattern.
     * Optimized to use a pre-compiled static pattern.
     * <p>
     * Valida el formato de un correo usando un patrón regex estricto.
     * Optimizado para usar un patrón estático pre-compilado.
     *
     * @param email The email to validate. / El correo a validar.
     * @return true if valid.
     */
    @Override
    public boolean validateEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if a username is available for registration.
     * <p>
     * Comprueba si un nombre de usuario está disponible para registro.
     *
     * @param username The username to check. / El nombre de usuario a comprobar.
     * @return true if available.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean UserNameAvailable(String username) {
        return userRepository.findUserNameAvailable(username);
    }


    /**
     * Retrieves the public profile of a user, including relationship status relative to the viewer.
     * <p>
     * Recupera el perfil público de un usuario, incluyendo el estado de la relación respecto al espectador.
     *
     * @param username    The username of the profile to view.
     *                    El nombre de usuario del perfil a ver.
     * @param currentUser The currently authenticated user (can be null).
     *                    El usuario actualmente autenticado (puede ser null).
     * @return The User DTO with profile details.
     *         El DTO de Usuario con detalles del perfil.
     * @throws EntityNotFoundException If the user is not found.
     *                                 Si el usuario no se encuentra.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDto getUserProfile(String username, User currentUser) {
        // 1. Find profile user
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado: " + username));

        boolean isFollowing = false;
        boolean isBlocked = false;

        // 2. Check relationships if viewer is authenticated and not looking at themselves
        if (currentUser != null && !currentUser.getId().equals(profileUser.getId())) {
            isFollowing = followRepository.existsByUserFollowerAndUserFollowed(currentUser, profileUser);
            // Check if CURRENT user has blocked the PROFILE user
            isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, profileUser);
        }

        // 3. Construct and return DTO
        return new UserDto(
                profileUser.getId(),
                profileUser.getUsername(),
                profileUser.getDisplayName(),
                profileUser.getAvatarUrl(defaultAvatarUrl),
                profileUser.getCalculatedThreadCount(),
                isFollowing,
                profileUser.getFollowingCount(),
                profileUser.getFollowersCount(),
                isBlocked,
                profileUser.getRole().name(),
                profileUser.isTwoFactorEnabled(),
                profileUser.getBackgroundType(),
                profileUser.getBackgroundValue()
        );
    }


    /**
     * Updates the user's theme preferences (background).
     * <p>
     * Actualiza las preferencias de tema del usuario (fondo).
     *
     * @param username The username. / El nombre de usuario.
     * @param dto      The preference data. / Los datos de preferencia.
     */
    @Override
    @Transactional
    public void updateThemePreferences(String username, UpdateThemeDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado"));

        user.setBackgroundType(dto.backgroundType());
        user.setBackgroundValue(dto.backgroundValue());

        userRepository.save(user);
    }

}
