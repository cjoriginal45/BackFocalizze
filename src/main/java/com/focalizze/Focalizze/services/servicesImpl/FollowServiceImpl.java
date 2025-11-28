package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.UserSummaryDto;
import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FollowService;
import com.focalizze.Focalizze.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final BlockRepository blockRepository;

    @Override
    @Transactional
    public void toggleFollowUser(String usernameToFollow, User currentUser) {
        // Buscamos al usuario que se quiere seguir/dejar de seguir.
        User userToFollow = userRepository.findByUsername(usernameToFollow)
                .orElseThrow(() -> new RuntimeException("Usuario a seguir no encontrado: " + usernameToFollow));

        // Un usuario no se puede seguir a si mismo
        if (currentUser.getId().equals(userToFollow.getId())) {
            throw new IllegalArgumentException("No puedes seguirte a ti mismo.");
        }

        boolean isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, userToFollow) ||
                blockRepository.existsByBlockerAndBlocked(userToFollow, currentUser);

        if (isBlocked) {
            throw new AccessDeniedException("No puedes seguir a este usuario debido a una restricción de bloqueo.");
        }

        // Comprobamos si la relación de seguimiento ya existe.
        Optional<Follow> existingFollow = followRepository
                .findByUserFollowerAndUserFollowed(currentUser, userToFollow);

        if (existingFollow.isPresent()) {
            // --- DEJAR DE SEGUIR ---
            followRepository.delete(existingFollow.get());

            // Actualizamos los contadores de forma atómica.
            userRepository.decrementFollowingCount(currentUser.getId());
            userRepository.decrementFollowersCount(userToFollow.getId());

        } else {
            // --- SEGUIR ---
            Follow newFollow = Follow.builder()
                    .userFollower(currentUser)
                    .userFollowed(userToFollow)
                    .createdAt(LocalDateTime.now())
                    .build();
            followRepository.save(newFollow);

            // Actualizamos los contadores de forma atómica.
            userRepository.incrementFollowingCount(currentUser.getId());
            userRepository.incrementFollowersCount(userToFollow.getId());

            // --- ENVIAR NOTIFICACIÓN ---
            notificationService.createAndSendNotification(
                    userToFollow,
                    NotificationType.NEW_FOLLOWER,
                    currentUser,
                    null
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowers(String targetUsername, User currentUser) {
        // 1. Obtenemos la lista de entidades User (los seguidores)
        List<User> followers = followRepository.findFollowersByUsername(targetUsername);
        return mapToUserSummaryDto(followers, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getFollowing(String targetUsername, User currentUser) {
        // 1. Obtenemos la lista de entidades User (los seguidos)
        List<User> following = followRepository.findFollowingByUsername(targetUsername);
        return mapToUserSummaryDto(following, currentUser);
    }

    private List<UserSummaryDto> mapToUserSummaryDto(List<User> users, User currentUser) {
        if (users.isEmpty()) return List.of();

        Set<Long> myFollowsIds;
        if (currentUser != null) {
            // Obtenemos los IDs de esta lista que YO ya sigo para marcar el isFollowing
            Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
            myFollowsIds = followRepository.findFollowedUserIdsByFollower(currentUser, userIds);
        } else {
            myFollowsIds = Set.of();
        }

        return users.stream().map(user -> new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl("assets/images/default-avatar.png"), // Manejo de nulo
                myFollowsIds.contains(user.getId())
        )).toList();
    }
}