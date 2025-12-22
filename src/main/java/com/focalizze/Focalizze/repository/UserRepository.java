package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing {@link User} entities.
 * Handles user authentication, profile lookups, and follower counters.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link User}.
 * Maneja la autenticación de usuarios, búsquedas de perfiles y contadores de seguidores.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     * <p>
     * Encuentra un usuario por su nombre de usuario.
     *
     * @param username The username. / El nombre de usuario.
     * @return Optional User.
     */
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * Finds a user by their email.
     * <p>
     * Encuentra un usuario por su correo electrónico.
     *
     * @param email The email address. / La dirección de correo.
     * @return Optional User.
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Finds a user by either username or email (for login flexibility).
     * <p>
     * Encuentra un usuario ya sea por nombre de usuario o correo (para flexibilidad en el inicio de sesión).
     *
     * @param username The username. / El nombre de usuario.
     * @param email    The email. / El correo.
     * @return Optional User.
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username,@Param("email") String email);

    /**
     * Checks if a username is available (i.e., not taken).
     * Returns true if count is 0.
     * <p>
     * Comprueba si un nombre de usuario está disponible (es decir, no está ocupado).
     * Devuelve true si el conteo es 0.
     *
     * @param username The username to check. / El nombre de usuario a verificar.
     * @return true if available. / true si está disponible.
     */
    @Query("SELECT COUNT(u) = 0 FROM User u WHERE u.username = :username")
    boolean findUserNameAvailable(@Param("username") String username);

    /**
     * Autocomplete helper: Finds up to 5 users starting with the prefix.
     * <p>
     * Ayuda de autocompletado: Encuentra hasta 5 usuarios que comienzan con el prefijo.
     *
     * @param prefix The search prefix. / El prefijo de búsqueda.
     * @return List of matching users. / Lista de usuarios coincidentes.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<User> findTop5ByUsernameStartingWithIgnoreCase(String prefix);


    // --- MÉTODOS DE ACTUALIZACIÓN DE CONTADORES ---

    /**
     * Atomically increments the following count.
     * <p>
     * Incrementa atómicamente el conteo de seguidos.
     *
     * @param userId The ID of the user. / El ID del usuario.
     */
    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount + 1 WHERE u.id = :userId")
    void incrementFollowingCount(@Param("userId") Long userId);

    /**
     * Atomically decrements the following count.
     * <p>
     * Decrementa atómicamente el conteo de seguidos.
     *
     * @param userId The ID of the user. / El ID del usuario.
     */
    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount - 1 WHERE u.id = :userId")
    void decrementFollowingCount(@Param("userId") Long userId);

    /**
     * Atomically increments the followers count.
     * <p>
     * Incrementa atómicamente el conteo de seguidores.
     *
     * @param userId The ID of the user. / El ID del usuario.
     */
    @Modifying
    @Query("UPDATE User u SET u.followersCount = u.followersCount + 1 WHERE u.id = :userId")
    void incrementFollowersCount(@Param("userId") Long userId);

    /**
     * Atomically decrements the followers count.
     * <p>
     * Decrementa atómicamente el conteo de seguidores.
     *
     * @param userId The ID of the user. / El ID del usuario.
     */
    @Modifying
    @Query("UPDATE User u SET u.followersCount = u.followersCount - 1 WHERE u.id = :userId")
    void decrementFollowersCount(@Param("userId") Long userId);

    /**
     * Retrieves all users matching a list of usernames.
     * <p>
     * Recupera todos los usuarios que coinciden con una lista de nombres de usuario.
     *
     * @param usernames Collection of usernames. / Colección de nombres de usuario.
     * @return List of users. / Lista de usuarios.
     */
    List<User> findAllByUsernameIn(Collection<String> usernames);

    // --- REEMPLAZA O AÑADE ESTE MÉTODO ---

    /**
     * Finds a user by ID and eagerly fetches 'following' and 'followedCategories'.
     * <p>
     * Busca un usuario por su ID y carga ("fetches") de forma proactiva (eager)
     * sus colecciones de 'following' y 'followedCategories'.
     *
     * @param id The ID of the user. / El ID del usuario a buscar.
     * @return Optional User with collections initialized. / Optional User con colecciones inicializadas.
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.following " +
            "LEFT JOIN FETCH u.followedCategories " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithFollows(@Param("id") Long id);

    /**
     * Finds a user by their password reset token.
     * <p>
     * Encuentra un usuario por su token de restablecimiento de contraseña.
     *
     * @param token The reset token. / El token de restablecimiento.
     * @return Optional User.
     */
    Optional<User> findByResetPasswordToken(String token);

    /**
     * Retrieves IDs of users blocked by a specific user.
     * <p>
     * Recupera IDs de usuarios bloqueados por un usuario específico.
     *
     * @param userId The blocker ID. / El ID del bloqueador.
     * @return Set of blocked IDs.
     */
    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId")
    Set<Long> findBlockedUserIdsByBlocker(@Param("userId") Long userId);

    /**
     * Retrieves IDs of users who blocked a specific user.
     * <p>
     * Recupera IDs de usuarios que bloquearon a un usuario específico.
     *
     * @param userId The blocked user ID. / El ID del usuario bloqueado.
     * @return Set of blocker IDs.
     */
    @Query("SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId")
    Set<Long> findUserIdsWhoBlockedUser(@Param("userId") Long userId);

    /**
     * Autocomplete helper: Finds top 5 users by username prefix, excluding specific IDs.
     * <p>
     * Ayuda de autocompletado: Encuentra los top 5 usuarios por prefijo de nombre, excluyendo IDs específicos.
     *
     * @param username The prefix. / El prefijo.
     * @param ids      IDs to exclude (e.g., already selected users). / IDs a excluir (ej. usuarios ya seleccionados).
     * @return List of users. / Lista de usuarios.
     */
    List<User> findTop5ByUsernameStartingWithIgnoreCaseAndIdNotIn(String username, Set<Long> ids);
}
