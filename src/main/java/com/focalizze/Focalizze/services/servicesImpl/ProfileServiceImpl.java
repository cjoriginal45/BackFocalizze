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
import jakarta.persistence.EntityNotFoundException;
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

/**
 * Implementation of the {@link ProfileService} interface.
 * Manages user profiles, avatar updates, and profile-specific thread retrieval.
 * <p>
 * Implementación de la interfaz {@link ProfileService}.
 * Gestiona perfiles de usuario, actualizaciones de avatar y recuperación de hilos específicos del perfil.
 */
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


    /**
     * Retrieves public profile information for a given username.
     * Includes context-aware data (isFollowing, isBlocked) relative to the viewer.
     * <p>
     * Recupera información de perfil público para un nombre de usuario dado.
     * Incluye datos conscientes del contexto (isFollowing, isBlocked) relativos al espectador.
     *
     * @param username The username to look up.
     *                 El nombre de usuario a buscar.
     * @return The profile DTO.
     *         El DTO del perfil.
     * @throws EntityNotFoundException If the user does not exist.
     *                                 Si el usuario no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(String username) {
        // 1. Get Profile User / Obtener Usuario del Perfil
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado: " + username));

        // 2. Fetch real-time counters / Obtener contadores en tiempo real
        long followersCount = followRepository.countByUserFollowed(profileUser);
        long followingCount = followRepository.countByUserFollower(profileUser);

        // 3. Get Current User Context / Obtener Contexto del Usuario Actual
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
            // Check Block Status / Verificar Estado de Bloqueo
            isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, profileUser) ||
                    blockRepository.existsByBlockerAndBlocked(profileUser, currentUser);

            // Check Following Status / Verificar Estado de Seguimiento
            isFollowing = followRepository.existsByUserFollowerAndUserFollowed(currentUser, profileUser);

            // Calculate threads availability only for own profile
            // Calcular disponibilidad de hilos solo para el propio perfil
            if (profileUser.getUsername().equals(currentUser.getUsername())) {
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

    /**
     * Retrieves threads created by a specific user (Profile Feed).
     * Filters out content if blocking exists between viewer and profile owner.
     * <p>
     * Recupera hilos creados por un usuario específico (Feed de Perfil).
     * Filtra contenido si existe bloqueo entre el espectador y el dueño del perfil.
     *
     * @param username The profile username.
     *                 El nombre de usuario del perfil.
     * @param pageable Pagination info.
     *                 Información de paginación.
     * @return A Page of enriched threads.
     *         Una Página de hilos enriquecidos.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getThreadsForUser(String username, Pageable pageable) {
        // Target Profile User / Usuario Perfil Objetivo
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado"));

        // Current Viewer (Safe cast check) / Espectador Actual (Verificación de cast segura)
        User currentUser = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            currentUser = (User) auth.getPrincipal();
        }

        // Check Block / Verificar Bloqueo
        if (currentUser != null) {
            boolean isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, profileUser) ||
                    blockRepository.existsByBlockerAndBlocked(profileUser, currentUser);

            if (isBlocked) {
                return Page.empty(pageable);
            }
        }

        // Fetch threads (Repository handles 'isDeleted' check)
        // Obtener hilos (El repositorio maneja la verificación 'isDeleted')
        Page<ThreadClass> threadPage = threadRepository.findByUserWithDetails(profileUser, pageable);

        // Enrich response / Enriquecer respuesta
        User finalCurrentUser = currentUser; // Final for lambda
        return threadPage.map(thread -> threadEnricher.enrich(thread, finalCurrentUser));
    }

    /**
     * Updates basic profile information (display name, biography).
     * <p>
     * Actualiza información básica del perfil (nombre para mostrar, biografía).
     *
     * @param username The username.
     *                 El nombre de usuario.
     * @param update   The DTO with new data.
     *                 El DTO con nuevos datos.
     * @return The updated profile DTO.
     *         El DTO del perfil actualizado.
     */
    @Override
    @Transactional
    public ProfileResponseDto updateProfile(String username, ProfileUpdateRequestDto update) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado"));

        if (update.displayName() != null && !update.displayName().isBlank()) {
            user.setDisplayName(update.displayName());
        }
        if (update.biography() != null) {
            user.setBiography(update.biography());
        }

        userRepository.save(user);

        return getProfile(username);
    }

    /**
     * Updates the user's avatar image.
     * Validates file type and presence.
     * <p>
     * Actualiza la imagen de avatar del usuario.
     * Valida el tipo de archivo y su presencia.
     *
     * @param username The username.
     *                 El nombre de usuario.
     * @param file     The image file.
     *                 El archivo de imagen.
     * @return The download URI of the new avatar.
     *         La URI de descarga del nuevo avatar.
     */
    @Override
    @Transactional
    public String updateAvatar(String username, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty / El archivo está vacío.");
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

    /**
     * Retrieves basic profile info for data export (GDPR compliance).
     * <p>
     * Recupera información básica del perfil para exportación de datos (cumplimiento GDPR).
     *
     * @param username The username.
     *                 El nombre de usuario.
     * @return Data download DTO.
     *         DTO de descarga de datos.
     */
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
