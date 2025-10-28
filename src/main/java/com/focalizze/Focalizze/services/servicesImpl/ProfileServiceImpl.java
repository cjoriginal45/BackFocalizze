package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ProfileUpdateRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.ProfileService;
import com.focalizze.Focalizze.services.ThreadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    private final FileStorageService fileStorageService;

    private static final int DAILY_THREAD_LIMIT = 3;

    public ProfileServiceImpl(UserRepository userRepository,
                              ThreadRepository threadRepository,
                              FollowRepository followRepository,
                              ThreadMapper threadMapper,
                              ThreadService threadService,
                              FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.followRepository = followRepository;
        this.threadMapper = threadMapper;
        this.threadService = threadService;
        this.fileStorageService = fileStorageService;
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
            long threadsCreatedToday = threadService.countByUserAndCreatedAtAfter(user,startOfToday);
            threadsAvailableToday = (Long) Math.max(0, DAILY_THREAD_LIMIT - threadsCreatedToday);
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
        return threadMapper.toDtoList(threadPage.getContent());
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
