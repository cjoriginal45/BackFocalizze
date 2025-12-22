package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.exceptions.DailyLimitExceededException;
import com.focalizze.Focalizze.models.*;
import com.focalizze.Focalizze.repository.*;
import com.focalizze.Focalizze.services.FileStorageService;
import com.focalizze.Focalizze.services.MentionService;
import com.focalizze.Focalizze.services.ThreadService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of the {@link ThreadService} interface.
 * Handles the lifecycle of threads: creation, retrieval, updates, and deletion.
 * <p>
 * Implementación de la interfaz {@link ThreadService}.
 * Maneja el ciclo de vida de los hilos: creación, recuperación, actualizaciones y eliminación.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ThreadServiceImpl implements ThreadService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadMapper threadMapper;
    private final FeedMapper feedMapper;
    private final SavedThreadRepository savedThreadRepository;
    private final MentionService mentionService;
    private static final int DAILY_THREAD_LIMIT = 3;
    private final FileStorageService fileStorageService;
    private final ThreadEnricher threadEnricher;


    /**
     * Calculates how many threads the user can still publish today.
     * <p>
     * Calcula cuántos hilos puede publicar el usuario hoy.
     *
     * @param user The user to check.
     *             El usuario a verificar.
     * @return Number of remaining threads allowed.
     *         Número de hilos restantes permitidos.
     */
    @Override
    @Transactional(readOnly = true)
    public int getThreadsAvailableToday(User user) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        // Uses the corrected query that ignores deleted threads
        // Usa la consulta corregida que ignora hilos eliminados
        long threadsCreatedToday = threadRepository.countActiveThreadsSince(user, startOfToday);
        return Math.max(0, DAILY_THREAD_LIMIT - (int) threadsCreatedToday);
    }


    /**
     * Creates a new thread with optional images and scheduled publication.
     * Validates daily limits and processes mentions.
     * <p>
     * Crea un nuevo hilo con imágenes opcionales y publicación programada.
     * Valida límites diarios y procesa menciones.
     *
     * @param requestDto The thread data.
     *                   Los datos del hilo.
     * @param files      Optional images.
     *                   Imágenes opcionales.
     * @return The created thread DTO.
     *         El DTO del hilo creado.
     */
    @Override
    @Transactional
    public ThreadResponseDto createThread(ThreadRequestDto requestDto, List<MultipartFile> files) {
        // 1. Get User
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found / Usuario no encontrado."));

        // 2. Validate Daily Limit
        if (getThreadsAvailableToday(currentUser) <= 0) {
            throw new DailyLimitExceededException("Límite diario de " + DAILY_THREAD_LIMIT + " hilos alcanzado.");
        }

        // 3. Resolve Category
        CategoryClass category = null;
        if (requestDto.category() != null && !requestDto.category().equalsIgnoreCase("Ninguna")) {
            category = categoryRepository.findByName(requestDto.category())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no válida"));
        }

        // 4. Setup Dates
        boolean isScheduled = requestDto.scheduledTime() != null;
        LocalDateTime creationTime = LocalDateTime.now();
        LocalDateTime publicationTime = isScheduled ? requestDto.scheduledTime() : creationTime;

        // 5. Build Thread Entity
        ThreadClass newThread = ThreadClass.builder()
                .user(currentUser)
                .category(category)
                .createdAt(creationTime)
                .isPublished(!isScheduled)
                .publishedAt(publicationTime)
                .scheduledTime(requestDto.scheduledTime())
                .isDeleted(false)
                .images(new HashSet<>())
                .build();

        // 6. Process Images
        if (files != null && !files.isEmpty()) {
            processImages(files, newThread, currentUser.getUsername());
        }


        // 7. Create Posts (Hardcoded positions as per DTO structure)
        Post post1 = Post.builder().content(requestDto.post1()).position(1).thread(newThread).build();
        Post post2 = Post.builder().content(requestDto.post2()).position(2).thread(newThread).build();
        Post post3 = Post.builder().content(requestDto.post3()).position(3).thread(newThread).build();
        newThread.setPosts(List.of(post1, post2, post3));

        // 8. Persist (Cascade handles posts and images)
        ThreadClass savedThread = threadRepository.save(newThread);

        // 9. Process Mentions
        savedThread.getPosts().forEach(post -> mentionService.processMentions(post, currentUser));

        return threadMapper.mapToResponseDto(savedThread);
    }

    // --- READ / LEER ---

    /**
     * Retrieves a thread by ID, increments its view count, and enriches it with user interaction data.
     * <p>
     * Recupera un hilo por ID, incrementa su contador de vistas y lo enriquece con datos de interacción del usuario.
     *
     * @param threadId The thread ID.
     *                 El ID del hilo.
     * @return Enriched feed DTO.
     *         DTO de feed enriquecido.
     */
    @Override
    @Transactional
    public FeedThreadDto getThreadByIdAndIncrementView(Long threadId) {
        // 1. Buscamos el hilo con toda su información
        // 1. We look for the thread with all its information
        ThreadClass thread = threadRepository.findByIdWithDetails(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found / Hilo no encontrado: " + threadId));

        if (thread.isDeleted()) {
            throw new EntityNotFoundException("Thread deleted / Hilo eliminado: " + threadId);
        }

        // 2. Increment view count
        thread.setViewCount(thread.getViewCount() + 1);
        ThreadClass updatedThread = threadRepository.save(thread);

        // 3. Enrich using the centralized component (DRY)
        User currentUser = null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            currentUser = user;
        }

        // 4. Mapeamos y enriquecemos el DTO para la respuesta
        // 4. We map and enrich the DTO for the response
        return threadEnricher.enrich(updatedThread, currentUser);
    }

    /**
     * Logically deletes a thread.
     * Verifies ownership before deletion.
     * <p>
     * Elimina lógicamente un hilo.
     * Verifica la propiedad antes de la eliminación.
     *
     * @param threadId    The thread ID.
     *                    El ID del hilo.
     * @param currentUser The user requesting deletion.
     *                    El usuario que solicita la eliminación.
     */
    @Override
    @Transactional
    public void deleteThread(Long threadId, User currentUser) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found / Hilo no encontrado: " + threadId));

        // Verificación de permisos: ¿El usuario actual es el autor del hilo?
        if (!thread.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No tienes permiso para borrar este hilo.");
        }

        // Logical Delete
        thread.setDeleted(true);
        threadRepository.save(thread);
    }

    /**
     * Updates an existing thread's posts or category.
     * <p>
     * Actualiza las publicaciones o la categoría de un hilo existente.
     *
     * @param threadId    The thread ID.
     *                    El ID del hilo.
     * @param updateDto   The new data.
     *                    Los nuevos datos.
     * @param currentUser The user requesting update.
     *                    El usuario que solicita la actualización.
     * @return The updated thread DTO.
     *         El DTO del hilo actualizado.
     */
    @Override
    public ThreadResponseDto updateThread(Long threadId, ThreadUpdateRequestDto updateDto, User currentUser) {
        // Fetch with posts eagerly
        ThreadClass thread = threadRepository.findByIdWithDetails(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Thread not found / Hilo no encontrado: " + threadId));

        // Permission Check
        if (!thread.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No tienes permiso para editar este hilo.");
        }

        // Actualizamos el contenido de los posts si se proporcionó en el DTO.
        List<Post> posts = thread.getPosts().stream()
                .sorted(Comparator.comparing(Post::getPosition))
                .toList();

        if (updateDto.post1() != null && !updateDto.post1().isBlank()) {
            posts.get(0).setContent(updateDto.post1());
        }
        if (posts.size() > 1 && updateDto.post2() != null && !updateDto.post2().isBlank()) {
            posts.get(1).setContent(updateDto.post2());
        }
        if (posts.size() > 2 && updateDto.post3() != null && !updateDto.post3().isBlank()) {
            posts.get(2).setContent(updateDto.post3());
        }

        // Update Category
        if (updateDto.categoryName() != null) {
            if (updateDto.categoryName().equalsIgnoreCase("Ninguna")) {
                thread.setCategory(null);
            } else {
                CategoryClass newCategory = categoryRepository.findByName(updateDto.categoryName())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid Category / Categoría no válida: " + updateDto.categoryName()));
                thread.setCategory(newCategory);
            }
        }

        ThreadClass threadSaved = threadRepository.save(thread);

        // Re-process mentions on update
        threadSaved.getPosts().forEach(post -> mentionService.processMentions(post, currentUser));

        // Devolvemos el hilo actualizado.
        return threadMapper.mapToResponseDto(threadSaved);
    }

    // Método privado refactorizado para evitar duplicar código
    // Private method refactored to avoid duplicate code
    private FeedThreadDto enrichDtoWithUserData(ThreadClass thread) {
        User currentUser = null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            currentUser = (User) authentication.getPrincipal();
        }

        FeedThreadDto dto = feedMapper.toFeedThreadDto(thread);

        boolean isLiked = false;
        boolean isSaved = false;

        if (currentUser != null) {
            final Long currentUserId = currentUser.getId();
            isLiked = thread.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUserId));
            isSaved = savedThreadRepository.existsByUserAndThread(currentUser, thread);
        }

        return new FeedThreadDto(
                dto.id(),
                dto.user(),
                dto.publicationDate(),
                dto.posts(),
                dto.stats(),
                isLiked,
                isSaved,
                dto.categoryName(),
                dto.images()
        );
    }

    private void processImages(List<MultipartFile> files, ThreadClass thread, String username) {
        for (MultipartFile file : files) {
            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                continue;
            }

            // Using the interface method properly (Avoiding cast)
            // Usando el método de la interfaz correctamente (Evitando cast)
            String fileName = fileStorageService.storeFile(file, username);

            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/images/")
                    .path(fileName)
                    .toUriString();

            ThreadImage image = ThreadImage.builder()
                    .imageUrl(fileUrl)
                    .thread(thread)
                    .build();

            thread.getImages().add(image);
        }
    }

}
