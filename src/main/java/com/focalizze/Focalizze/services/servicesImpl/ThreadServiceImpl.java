package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.ThreadUpdateRequestDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.exceptions.DailyLimitExceededException;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.MentionService;
import com.focalizze.Focalizze.services.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

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
    public ThreadResponseDto createThread(ThreadRequestDto requestDto) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado."));

        // VALIDACIÓN: Usamos el método que ignora los hilos borrados
        if (getThreadsAvailableToday(currentUser) <= 0) {
            throw new DailyLimitExceededException("Límite diario de " + DAILY_THREAD_LIMIT + " hilos alcanzado.");
        }

        CategoryClass category = null;
        String categoryName = requestDto.category();

        if (categoryName != null && !categoryName.equalsIgnoreCase("Ninguna")) {
            category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no válida: " + categoryName));
        }

        boolean isScheduled = requestDto.scheduledTime() != null;
        LocalDateTime creationTime = LocalDateTime.now();
        LocalDateTime publicationTime = isScheduled ? requestDto.scheduledTime() : creationTime;

        ThreadClass newThread = ThreadClass.builder()
                .user(currentUser)
                .category(category)
                .createdAt(creationTime)
                .isPublished(!isScheduled)
                .publishedAt(publicationTime)
                .scheduledTime(requestDto.scheduledTime())
                .isDeleted(false) // Nace vivo
                .likeCount(0)
                .commentCount(0)
                .saveCount(0)
                .viewCount(0)
                .build();

        Post post1 = Post.builder().content(requestDto.post1()).position(1).thread(newThread).build();
        Post post2 = Post.builder().content(requestDto.post2()).position(2).thread(newThread).build();
        Post post3 = Post.builder().content(requestDto.post3()).position(3).thread(newThread).build();

        newThread.setPosts(List.of(post1, post2, post3));

        ThreadClass savedThread = threadRepository.save(newThread);

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
        // Obtenemos el usuario actual (o null si es un invitado)
        // Get the current user (or null if it's a guest)
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
                dto.id(), dto.user(), dto.publicationDate(), dto.posts(),
                dto.stats(), isLiked, isSaved, dto.categoryName()
        );
    }

}
