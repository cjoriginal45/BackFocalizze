package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FollowRepository extends JpaRepository<Follow,Long> {

    long countByUserFollowed(User user);

    long countByUserFollower(User user);

    Optional<Follow> findByUserFollowerAndUserFollowed(User follower, User followed);

    @Query("SELECT f.userFollowed.id FROM Follow f WHERE f.userFollower = :follower " +
            "AND f.userFollowed.id IN :followedIds")
    Set<Long> findFollowedUserIdsByFollower(@Param("follower") User follower,
                                            @Param("followedIds") Set<Long> followedIds);

    boolean existsByUserFollowerAndUserFollowed(User follower, User followed);

    // Obtener la lista de usuarios QUE SIGUEN a un usuario (Seguidores)
    @Query("SELECT f.userFollower FROM Follow f WHERE f.userFollowed.username = :username ORDER BY f.createdAt DESC")
    List<User> findFollowersByUsername(@Param("username") String username);

    // Obtener la lista de usuarios A LOS QUE SIGUE un usuario (Seguidos)
    @Query("SELECT f.userFollowed FROM Follow f WHERE f.userFollower.username = :username ORDER BY f.createdAt DESC")
    List<User> findFollowingByUsername(@Param("username") String username);
}