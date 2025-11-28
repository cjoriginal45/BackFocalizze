package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ProfileUpdateRequestDto;
import com.focalizze.Focalizze.dto.UserProfileDownloadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.ProfileService;
import com.focalizze.Focalizze.services.ThreadService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final FollowRepository followRepository;
    private final FileStorageService fileStorageService;
    private final ThreadEnricher threadEnricher;
    private static final int DAILY_THREAD_LIMIT = 3;
    private final BlockRepository blockRepository;

    private final ThreadService threadService;

    @Value("${app.default-avatar-url}")
    private String defaultAvatarUrl;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(String username) {
        // 1. Obtener el usuario del perfil que se está visitando.
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        // 2. Obtener los contadores objetivos del perfil.
        long followersCount = followRepository.countByUserFollowed(profileUser);
        long followingCount = followRepository.countByUserFollower(profileUser);

        // 3. Obtener el contexto de autenticación del usuario que está haciendo la petición.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // --- Lógica para datos que dependen del espectador ---
        Long threadsAvailableToday = null;
        boolean isFollowing = false;

        // Obtener currentUser de forma segura para verificar bloqueos
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User) {
            currentUser = (User) authentication.getPrincipal();
        }

        boolean isBlocked = false;
        if (currentUser != null) {
            isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, profileUser) ||
                    blockRepository.existsByBlockerAndBlocked(profileUser, currentUser);
        }

        // 4. Lógica si el espectador está autenticado.
        if (currentUser != null) {

            // a) Calculamos 'isFollowing'
            isFollowing = followRepository.existsByUserFollowerAndUserFollowed(currentUser, profileUser);

            // b) Calculamos 'threadsAvailableToday' SÓLO si el espectador es el dueño del perfil.
            if (profileUser.getUsername().equals(currentUser.getUsername())) {
                // USAMOS EL MÉTODO CENTRALIZADO DEL THREADSERVICE
                // Esto garantiza que el cálculo sea consistente con la lógica de creación/borrado
                threadsAvailableToday = (long) threadService.getThreadsAvailableToday(currentUser);
            }
        }

        // 5. Construir y devolver el DTO de respuesta final.
        return new ProfileResponseDto(
                profileUser.getId(),
                profileUser.getUsername(),
                profileUser.getDisplayName(),
                profileUser.getAvatarUrl(defaultAvatarUrl),
                profileUser.getBiography(),
                (int) followersCount,
                (int) followingCount,
                profileUser.getCalculatedThreadCount(),
                threadsAvailableToday,
                profileUser.getCreatedAt(),
                isFollowing,
                profileUser.getFollowingCount(),
                profileUser.getFollowersCount(),
                isBlocked
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getThreadsForUser(String username, Pageable pageable) {
        // Obtener el usuario del perfil que se está visitando.
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener el usuario que está viendo la página (el que está logueado).
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Verificar que el usuario no este bloqueado
        boolean isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, profileUser) ||
                blockRepository.existsByBlockerAndBlocked(profileUser, currentUser);

        if (isBlocked) {
            // Si hay un bloqueo, simplemente devolvemos una página vacía.
            return Page.empty(pageable);
        }

        // Buscamos los hilos del 'profileUser'.
        // Asegúrate de que tu repositorio filtre 'isDeleted = false' en esta consulta,
        // o usa una consulta personalizada si no lo hace por defecto.
        Page<ThreadClass> threadPage = threadRepository.findByUserWithDetails(profileUser, pageable);

        // Enriquecemos la respuesta usando el 'currentUser' como contexto.
        return threadPage.map(thread -> threadEnricher.enrich(thread, currentUser));
    }

    @Override
    public ProfileResponseDto updateProfile(String username, ProfileUpdateRequestDto update) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (update.displayName() != null && !update.displayName().isBlank()) {
            user.setDisplayName(update.displayName());
        }
        if (update.biography() != null) {
            user.setBiography(update.biography());
        }

        userRepository.save(user);

        return getProfile(username);
    }

    @Override
    @Transactional
    public String updateAvatar(String username, MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            throw new RuntimeException("Formato de archivo no válido. Solo se permiten JPG, PNG y WEBP.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String filename = fileStorageService.storeFile(file, username);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/profiles/avatars/")
                .path(filename)
                .toUriString();

        user.setAvatarUrl(fileDownloadUri);
        userRepository.save(user);

        return fileDownloadUri;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDownloadDto getProfileForDownload(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        return new UserProfileDownloadDto(
                user.getUsername(),
                user.getAvatarUrl(defaultAvatarUrl),
                user.getBiography()
        );
    }
}
