package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.ProfileService;
import com.focalizze.Focalizze.services.ThreadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final FollowRepository followRepository;
    private final ThreadMapper threadMapper;
    private final ThreadService threadService;

    public ProfileServiceImpl(UserRepository userRepository,
                              ThreadRepository threadRepository,
                              FollowRepository followRepository,
                              ThreadMapper threadMapper,
                              ThreadService threadService) {
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.followRepository = followRepository;
        this.threadMapper = threadMapper;
        this.threadService = threadService;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(String username) {
        // 1. Obtener el usuario del perfil
        // 1. Get the profile user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        // 2. Obtener los contadores usando los nuevos métodos del repositorio
        // 2. Get the counters using the new repository methods
        long followersCount = followRepository.countByUserFollowed(user);
        long followingCount = followRepository.countByUserFollower(user);
        long threadCount = threadRepository.countByUser(user);

        // 3. Lógica para los hilos disponibles
        // 3. Logic for available threads
        Long threadsAvailableToday = null;
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username.equals(currentUsername)) {
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            threadsAvailableToday = threadService.countByUserAndCreatedAtAfter(user,startOfToday);
        }

        // 4. Construir y devolver el DTO de respuesta
        // 4. Build and return the response DTO
        return new ProfileResponseDto(
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBiography(),
                (int) followersCount,
                (int) followingCount,
                (int) threadCount,
                threadsAvailableToday,
                user.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThreadResponseDto> getThreadsForUser(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Page<ThreadClass> threadPage = threadRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        // Mapear la página de entidades a una lista de DTOs
        return threadMapper.toDtoList(List.of((ThreadClass) threadPage.getContent())); // Placeholder, aquí usarías tu ThreadMapper
    }
}
