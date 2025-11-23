package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ProfileUpdateRequestDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.ProfileService;
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

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
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
        long threadCount = threadRepository.countByUser(profileUser);

        // 3. Obtener el contexto de autenticación del usuario que está haciendo la petición.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // --- Lógica para datos que dependen del espectador ---
        Long threadsAvailableToday = null;
        boolean isFollowing = false;

        // 4. Comprobamos si el espectador está autenticado.
        //    Si no lo está, 'authentication' será nulo o no estará autenticado,
        //    y 'getPrincipal()' devolverá "anonymousUser".
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User currentUser) {

            // El espectador SÍ está autenticado. Ahora podemos calcular los datos subjetivos.

            // a) Calculamos 'isFollowing'
            //    Verificamos si el usuario autenticado (currentUser) sigue al usuario del perfil (profileUser).
            isFollowing = followRepository.existsByUserFollowerAndUserFollowed(currentUser, profileUser);

            // b) Calculamos 'threadsAvailableToday' SÓLO si el espectador es el dueño del perfil.
            if (profileUser.getUsername().equals(currentUser.getUsername())) {
                LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
                long threadsCreatedToday = threadRepository
                        .countByUserAndCreatedAtAfter(currentUser, startOfToday);
                threadsAvailableToday = Math.max(0L, DAILY_THREAD_LIMIT - threadsCreatedToday);
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
                profileUser.getFollowersCount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getThreadsForUser(String username, Pageable pageable) {
        // 1. Obtener el usuario del perfil que se está visitando.
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Obtener el usuario que está viendo la página (el que está logueado).
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 3. Buscamos los hilos del 'profileUser'.
        Page<ThreadClass> threadPage = threadRepository.findByUserWithDetails(profileUser, pageable);

        // 4. Enriquecemos la respuesta usando el 'currentUser' como contexto.
        return threadPage.map(thread -> threadEnricher.enrich(thread, currentUser));
    }

    @Override
    public ProfileResponseDto updateProfile(String username,ProfileUpdateRequestDto update) {
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
        // 1. Validar el archivo
        // 1. Validate the file
        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío.");
        }
        //validar tipo de archivo
        //validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            throw new RuntimeException("Formato de archivo no válido. Solo se permiten JPG, PNG y WEBP.");
        }

        // 2. Obtener el usuario
        // 2. Get the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Guardar el archivo en el disco usando el FileStorageService
        //    Pasamos el username para generar un nombre de archivo único.
        // 3. Save the file to disk using the FileStorageService
        // We pass the username to generate a unique name.
        String filename = fileStorageService.storeFile(file, username);

        // 4. Construir la URL completa que el frontend usará para acceder a la imagen
        // 4. Build the full URL that the frontend will use to access the image
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/profiles/avatars/")
                .path(filename)
                .toUriString();

        // 5. Actualizar la entidad User con la nueva URL del avatar
        // 5. Update the User entity with the new avatar URL
        user.setAvatarUrl(fileDownloadUri);
        userRepository.save(user);

        // 6. Devolver la URL completa
        // 6. Return the full URL
        return fileDownloadUri;
    }
}
