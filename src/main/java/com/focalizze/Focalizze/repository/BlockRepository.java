package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Block;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface BlockRepository extends JpaRepository<Block, Long> {
    // Método para encontrar una relación de bloqueo específica.
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    // Método para comprobar rápidamente si un bloqueo existe.
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    Set<Long> findBlockedUserIdsByBlocker(Long id);

    Set<Long> findUserIdsWhoBlockedUser(Long id);
}
