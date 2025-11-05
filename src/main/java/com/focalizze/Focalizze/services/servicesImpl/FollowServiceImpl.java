package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.Follow;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Override
    @Transactional
    public void toggleFollowUser(String usernameToFollow, User currentUser) {
        // 1. Buscamos al usuario que se quiere seguir/dejar de seguir.
        User userToFollow = userRepository.findByUsername(usernameToFollow)
                .orElseThrow(() -> new RuntimeException("Usuario a seguir no encontrado: " + usernameToFollow));

        // Un usuario no se puede seguir a si mismo
        if (currentUser.getId().equals(userToFollow.getId())) {
            throw new IllegalArgumentException("No puedes seguirte a ti mismo.");
        }
        // 2. Comprobamos si la relación de seguimiento ya existe.
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
        }
    }
}
