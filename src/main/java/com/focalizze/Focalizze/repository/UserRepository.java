package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username,@Param("email") String email);

    @Query("SELECT COUNT(u) = 0 FROM User u WHERE u.username = :username")
    boolean findUserNameAvailable(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<User> findTop5ByUsernameStartingWithIgnoreCase(String prefix);


    // --- MÉTODOS DE ACTUALIZACIÓN DE CONTADORES ---
    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount + 1 WHERE u.id = :userId")
    void incrementFollowingCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount - 1 WHERE u.id = :userId")
    void decrementFollowingCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.followersCount = u.followersCount + 1 WHERE u.id = :userId")
    void incrementFollowersCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.followersCount = u.followersCount - 1 WHERE u.id = :userId")
    void decrementFollowersCount(@Param("userId") Long userId);

    List<User> findAllByUsernameIn(Collection<String> usernames);

    // --- REEMPLAZA O AÑADE ESTE MÉTODO ---
    /**
     * Busca un usuario por su ID y carga ("fetches") de forma proactiva (eager)
     * sus colecciones de 'following' y 'followedCategories' en la misma consulta.
     * Esto previene la LazyInitializationException.
     *
     * @param id El ID del usuario a buscar.
     * @return Un Optional que contiene el User con sus colecciones cargadas.
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.following " +
            "LEFT JOIN FETCH u.followedCategories " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithFollows(@Param("id") Long id);

    Optional<User> findByResetPasswordToken(String token);

    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId")
    Set<Long> findBlockedUserIdsByBlocker(@Param("userId") Long userId);

    @Query("SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId")
    Set<Long> findUserIdsWhoBlockedUser(@Param("userId") Long userId);
}
