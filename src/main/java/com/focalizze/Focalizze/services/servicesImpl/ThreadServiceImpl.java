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

@Service
@RequiredArgsConstructor
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


    // --- MÉTODO PARA OBTENER DISPONIBLES ---
    @Override
    @Transactional(readOnly = true)
    public int getThreadsAvailableToday(User user) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        // USAMOS LA QUERY CORREGIDA QUE IGNORA BORRADOS
        long threadsCreatedToday = threadRepository.countActiveThreadsSince(user, startOfToday);
        return Math.max(0, DAILY_THREAD_LIMIT - (int) threadsCreatedToday);
    }


    //Logica de negocio para crear un hilo
    //Business logic to create a thread
    @Override
    @Transactional
    public ThreadResponseDto createThread(ThreadRequestDto requestDto, List<MultipartFile> files) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado."));

        // Validar límite diario (Tu lógica existente)
        if (getThreadsAvailableToday(currentUser) <= 0) {
            throw new DailyLimitExceededException("Límite diario de " + DAILY_THREAD_LIMIT + " hilos alcanzado.");
        }

        // ... (Lógica de categoría y fechas igual que antes) ...
        CategoryClass category = null;
        if (requestDto.category() != null && !requestDto.category().equalsIgnoreCase("Ninguna")) {
            category = categoryRepository.findByName(requestDto.category())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no válida"));
        }

        boolean isScheduled = requestDto.scheduledTime() != null;
        LocalDateTime creationTime = LocalDateTime.now();
        LocalDateTime publicationTime = isScheduled ? requestDto.scheduledTime() : creationTime;

        // 1. Crear el Hilo
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

        // 2. Procesar Imágenes (NUEVO)
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                // Validar tipo (opcional, también se hace en front)
                if (!file.getContentType().startsWith("image/")) continue;

                // Guardar en disco


                String fileName = ((FileStorageServiceImpl) fileStorageService).storeThreadImage(file);

                // Generar URL (Ajusta la ruta según tu configuración de recursos estáticos)
                String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/images/") // O la ruta que exponga tu carpeta de uploads
                        .path(fileName)
                        .toUriString();

                // Crear entidad imagen
                ThreadImage image = ThreadImage.builder()
                        .imageUrl(fileUrl)
                        .thread(newThread)
                        .build();

                newThread.getImages().add(image);
            }
        }

        // 3. Crear Posts
        Post post1 = Post.builder().content(requestDto.post1()).position(1).thread(newThread).build();
        Post post2 = Post.builder().content(requestDto.post2()).position(2).thread(newThread).build();
        Post post3 = Post.builder().content(requestDto.post3()).position(3).thread(newThread).build();
        newThread.setPosts(List.of(post1, post2, post3));

        // 4. Guardar todo (Cascade se encarga de posts e imágenes)
        ThreadClass savedThread = threadRepository.save(newThread);

        // Menciones
        for (Post post : savedThread.getPosts()) {
            mentionService.processMentions(post, currentUser);
        }

        return threadMapper.mapToResponseDto(savedThread);
    }

    // Método para obtener detalles e incrementar vistas
    // Method to get details and increment views
    @Override
    @Transactional
    public FeedThreadDto getThreadByIdAndIncrementView(Long threadId) {
        // 1. Buscamos el hilo con toda su información
        // 1. We look for the thread with all its information
        ThreadClass thread = threadRepository.findByIdWithDetails(threadId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró un hilo con el ID: " + threadId));

        if (thread.isDeleted()) {
            throw new NoSuchElementException("Hilo eliminado: " + threadId);
        }
        // 2. Incrementamos el contador de vistas
        // 2. We increase the view counter
        thread.setViewCount(thread.getViewCount() + 1);

        // 3. Guardamos el cambio (la transacción se encargará de persistirlo)
        // 3. Save the change (the transaction will take care of persisting it)
        ThreadClass updatedThread = threadRepository.save(thread);

        // 4. Mapeamos y enriquecemos el DTO para la respuesta
        // 4. We map and enrich the DTO for the response
        return enrichDtoWithUserData(updatedThread);
    }

    @Override
    @Transactional
    public void deleteThread(Long threadId, User currentUser) {
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new NoSuchElementException("Hilo no encontrado con ID: " + threadId));

        // Verificación de permisos: ¿El usuario actual es el autor del hilo?
        if (!thread.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No tienes permiso para borrar este hilo.");
        }

        // Realizamos el borrado lógico.
        thread.setDeleted(true);

        // Guardamos la entidad actualizada.
        threadRepository.save(thread);
    }

    @Override
    public ThreadResponseDto updateThread(Long threadId, ThreadUpdateRequestDto updateDto, User currentUser) {
        ThreadClass thread = threadRepository.findByIdWithDetails(threadId) // Usamos la consulta que hace JOIN FETCH a posts
                .orElseThrow(() -> new NoSuchElementException("Hilo no encontrado con ID: " + threadId));

        // Verificación de permisos.
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

        // Comprobamos si se proporcionó un nuevo nombre de categoría en el DTO.
        if (updateDto.categoryName() != null) {
            // Si el nombre es "Ninguna", significa que el usuario quiere quitar la categoría.
            if (updateDto.categoryName().equalsIgnoreCase("Ninguna")) {
                thread.setCategory(null);
            } else {
                // Si es otro nombre, buscamos la categoría en la base de datos.
                CategoryClass newCategory = categoryRepository.findByName(updateDto.categoryName())
                        .orElseThrow(() -> new IllegalArgumentException("Categoría no válida: " + updateDto.categoryName()));
                // Asignamos la nueva categoría al hilo.
                thread.setCategory(newCategory);
            }
        }

        ThreadClass threadSaved = threadRepository.save(thread);

        //menciones
        for (Post post : thread.getPosts()) {
            mentionService.processMentions(post, currentUser);
        }

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



}
