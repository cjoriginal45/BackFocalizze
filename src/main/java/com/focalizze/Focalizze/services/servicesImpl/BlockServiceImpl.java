package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.BlockedUserDto;
import com.focalizze.Focalizze.models.Block;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.BlockService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link BlockService} interface.
 * Handles the logic for blocking/unblocking users and the side effects (removing follow relationships).
 * <p>
 * Implementación de la interfaz {@link BlockService}.
 * Maneja la lógica para bloquear/desbloquear usuarios y los efectos secundarios (eliminar relaciones de seguimiento).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    /**
     * Toggles the block status between the current user and a target user.
     * If blocked: unblocks.
     * If not blocked: blocks and removes any mutual follow relationships (and updates counters).
     * <p>
     * Alterna el estado de bloqueo entre el usuario actual y un usuario objetivo.
     * Si está bloqueado: desbloquea.
     * Si no está bloqueado: bloquea y elimina cualquier relación de seguimiento mutua (y actualiza contadores).
     *
     * @param usernameToToggle The username of the user to block/unblock.
     *                         El nombre de usuario del usuario a bloquear/desbloquear.
     * @return {@code true} if the user is now blocked, {@code false} if unblocked.
     *         {@code true} si el usuario ahora está bloqueado, {@code false} si está desbloqueado.
     * @throws EntityNotFoundException If the user to toggle is not found.
     *                                 Si no se encuentra el usuario a alternar.
     * @throws IllegalArgumentException If trying to block oneself.
     *                                  Si se intenta bloquear a uno mismo.
     */
    @Override
    public boolean toggleBlock(String usernameToToggle) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found / Usuario autenticado no encontrado"));

        User userToToggle = userRepository.findByUsername(usernameToToggle)
                .orElseThrow(() -> new EntityNotFoundException("User to block not found / Usuario a bloquear no encontrado: " + usernameToToggle));

        if (currentUser.equals(userToToggle)) {
            throw new IllegalArgumentException("You cannot block yourself / No puedes bloquearte a ti mismo.");
        }

        // Comprobar si ya existe una relación de bloqueo
        Optional<Block> existingBlock = blockRepository.findByBlockerAndBlocked(currentUser, userToToggle);

        if (existingBlock.isPresent()) {
            // UNBLOCK Logic
            // Lógica de DESBLOQUEO
            blockRepository.delete(existingBlock.get());
            return false;
        } else {
            // BLOCK Logic
            // Lógica de BLOQUEO
            Block newBlock = Block.builder()
                    .blocker(currentUser)
                    .blocked(userToToggle)
                    .createdAt(LocalDateTime.now())
                    .build();
            blockRepository.save(newBlock);

            // Side Effect: Remove Follows and Update Counters
            // Efecto Secundario: Eliminar Seguimientos y Actualizar Contadores

            // A. Remove Current -> Target follow
            long unfollowedCount1 = followRepository.deleteFollowRelation(currentUser, userToToggle);
            if (unfollowedCount1 > 0) {
                userRepository.decrementFollowingCount(currentUser.getId());
                userRepository.decrementFollowersCount(userToToggle.getId());
            }

            // B. Remove Target -> Current follow
            long unfollowedCount2 = followRepository.deleteFollowRelation(userToToggle, currentUser);
            if (unfollowedCount2 > 0) {
                userRepository.decrementFollowingCount(userToToggle.getId());
                userRepository.decrementFollowersCount(currentUser.getId());
            }

            return true;
        }
    }

    /**
     * Retrieves a list of users blocked by the currently authenticated user.
     * <p>
     * Recupera una lista de usuarios bloqueados por el usuario actualmente autenticado.
     *
     * @return A list of {@link BlockedUserDto}.
     *         Una lista de {@link BlockedUserDto}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserDto> getBlockedUsers() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found / Usuario autenticado no encontrado"));

        List<User> blockedUsers = blockRepository.findBlockedUsersByBlocker(currentUser);

        // Map User entities to DTOs
        // Mapear entidades User a DTOs
        return blockedUsers.stream()
                .map(user -> new BlockedUserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getAvatarUrl(defaultAvatarUrl)
                ))
                .toList();
    }

}
