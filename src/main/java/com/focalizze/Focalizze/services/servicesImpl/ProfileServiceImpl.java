package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.ProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final ThreadMapper threadMapper;

    public ProfileServiceImpl(UserRepository userRepository, ThreadRepository threadRepository, ThreadMapper threadMapper) {
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.threadMapper = threadMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        long threadCount = threadRepository.countByUser(user);
        // Lógica para contar seguidores/seguidos (necesitarías un FollowRepository)
        // Integer followers = followRepository.countByFollowed(user);
        // Integer following = followRepository.countByFollower(user);

        Integer threadsAvailableToday = null;
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username.equals(currentUsername)) {
            // Lógica para calcular hilos disponibles (ej: 5 - hilos creados hoy)
            threadsAvailableToday = 3; // Placeholder
        }

        return new ProfileResponseDto(
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBiography(),
                // followers,
                // following,
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
        // return threadMapper.toDtoList(threadPage.getContent());
        return threadMapper.toDtoList(List.of((ThreadClass) threadPage.getContent())); // Placeholder, aquí usarías tu ThreadMapper
    }
}
