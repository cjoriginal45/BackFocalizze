package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Block;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BlockRepository extends JpaRepository<Block, Long> {
    // Método para encontrar una relación de bloqueo específica.
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    // Método para comprobar rápidamente si un bloqueo existe.
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId")
    Set<Long> findBlockedUserIdsByBlocker(@Param("userId") Long userId);

    @Query("SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId")
    Set<Long> findUserIdsWhoBlockedUser(@Param("userId") Long userId);

    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker = :blocker AND b.blocked.id IN :authorIds")
    Set<Long> findBlockedIdsByBlockerAndBlockedIdsIn(
            @Param("blocker") User blocker,
            @Param("authorIds") Set<Long> authorIds
    );

    @Query("SELECT b.blocked FROM Block b WHERE b.blocker = :blocker ORDER BY b.createdAt DESC")
    List<User> findBlockedUsersByBlocker(@Param("blocker") User blocker);
}
