package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing {@link Follow} entities.
 * Handles the relationship between users (Follower -> Followed).
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link Follow}.
 * Maneja la relación entre usuarios (Seguidor -> Seguido).
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow,Long> {

    /**
     * Counts the number of followers a user has.
     * <p>
     * Cuenta el número de seguidores que tiene un usuario.
     *
     * @param user The user being followed.
     *             El usuario que está siendo seguido.
     * @return The count of followers.
     *         El conteo de seguidores.
     */
    long countByUserFollowed(User user);

    /**
     * Counts the number of users a specific user follows.
     * <p>
     * Cuenta el número de usuarios que sigue un usuario específico.
     *
     * @param user The user who is following others.
     *             El usuario que sigue a otros.
     * @return The count of followed users.
     *         El conteo de usuarios seguidos.
     */
    long countByUserFollower(User user);

    /**
     * Finds a specific follow relationship.
     * <p>
     * Encuentra una relación de seguimiento específica.
     *
     * @param follower The user who is following.
     *                 El usuario que sigue.
     * @param followed The user being followed.
     *                 El usuario que es seguido.
     * @return An {@link Optional} containing the Follow entity if it exists.
     *         Un {@link Optional} que contiene la entidad Follow si existe.
     */
    Optional<Follow> findByUserFollowerAndUserFollowed(User follower, User followed);

    /**
     * Filters a list of potential user IDs, returning only those that the follower is actually following.
     * Optimized projection to return IDs only.
     * <p>
     * Filtra una lista de IDs de usuarios potenciales, devolviendo solo aquellos que el seguidor realmente sigue.
     * Proyección optimizada para devolver solo IDs.
     *
     * @param follower    The user who performs the follow check.
     *                    El usuario que realiza la verificación de seguimiento.
     * @param followedIds The set of candidate IDs to check.
     *                    El conjunto de IDs candidatos a verificar.
     * @return A {@link Set} of confirmed followed IDs.
     *         Un {@link Set} de IDs seguidos confirmados.
     */
    @Query("SELECT f.userFollowed.id FROM Follow f WHERE f.userFollower = :follower " +
            "AND f.userFollowed.id IN :followedIds")
    Set<Long> findFollowedUserIdsByFollower(@Param("follower") User follower,
                                            @Param("followedIds") Set<Long> followedIds);

    /**
     * Checks if a follow relationship exists efficiently.
     * <p>
     * Comprueba si existe una relación de seguimiento de manera eficiente.
     *
     * @param follower The user who is following.
     *                 El usuario que sigue.
     * @param followed The user being followed.
     *                 El usuario que es seguido.
     * @return {@code true} if the relationship exists.
     *         {@code true} si la relación existe.
     */
    boolean existsByUserFollowerAndUserFollowed(User follower, User followed);

    /**
     * Deletes a follow relationship directly in the database.
     * More efficient than finding and then deleting the entity.
     * <p>
     * Elimina una relación de seguimiento directamente en la base de datos.
     * Más eficiente que buscar y luego eliminar la entidad.
     *
     * @param follower The user who wants to unfollow.
     *                 El usuario que quiere dejar de seguir.
     * @param followed The user to be unfollowed.
     *                 El usuario a dejar de seguir.
     * @return The number of records deleted (usually 1 or 0).
     *         El número de registros eliminados (usualmente 1 o 0).
     */
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.userFollower = :follower AND f.userFollowed = :followed")
    int deleteFollowRelation(@Param("follower") User follower, @Param("followed") User followed);

    /**
     * Retrieves the list of users who follow a specific user (Followers), ordered by most recent.
     * Uses explicit joins for better query optimization.
     * <p>
     * Recupera la lista de usuarios que siguen a un usuario específico (Seguidores), ordenados por el más reciente.
     * Utiliza joins explícitos para una mejor optimización de la consulta.
     *
     * @param username The username of the user whose followers are requested.
     *                 El nombre de usuario del usuario cuyos seguidores se solicitan.
     * @return A {@link List} of follower {@link User} entities.
     *         Una {@link List} de entidades {@link User} seguidoras.
     */
    @Query("SELECT f.userFollower FROM Follow f JOIN f.userFollowed u " +
            "WHERE u.username = :username ORDER BY f.createdAt DESC")
    List<User> findFollowersByUsername(@Param("username") String username);

    /**
     * Retrieves the list of users that a specific user follows (Following), ordered by most recent.
     * Uses explicit joins for better query optimization.
     * <p>
     * Recupera la lista de usuarios a los que sigue un usuario específico (Seguidos), ordenados por el más reciente.
     * Utiliza joins explícitos para una mejor optimización de la consulta.
     *
     * @param username The username of the user who is following.
     *                 El nombre de usuario del usuario que está siguiendo.
     * @return A {@link List} of followed {@link User} entities.
     *         Una {@link List} de entidades {@link User} seguidas.
     */
    @Query("SELECT f.userFollowed FROM Follow f JOIN f.userFollower u " +
            "WHERE u.username = :username ORDER BY f.createdAt DESC")
    List<User> findFollowingByUsername(@Param("username") String username);
}
