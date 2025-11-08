package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
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
import com.focalizze.Focalizze.services.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private static final int DAILY_THREAD_LIMIT = 3;


    //Logica de negocio para crear un hilo
    //Business logic to create a thread
    @Override
    @Transactional
    public ThreadResponseDto createThread(ThreadRequestDto requestDto) {
        // Obtener el usuario autenticado desde el contexto de seguridad
        // Get the authenticated user from the security context
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado, no se puede crear el hilo."));

        // Calcular cuántos hilos ha creado el usuario hoy
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long threadsCreatedToday = threadRepository.countByUserAndCreatedAtAfter(currentUser, startOfToday);

        if (threadsCreatedToday >= DAILY_THREAD_LIMIT) {
            // Si se alcanza el límite, lanzamos la excepción y la ejecución se detiene aquí.
            throw new DailyLimitExceededException("Límite diario de " + DAILY_THREAD_LIMIT + " hilos alcanzado.");
        }

        CategoryClass category = null; // Inicia como null
        String categoryName = requestDto.category();

        if (categoryName != null && !categoryName.equalsIgnoreCase("Ninguna")) {
            category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no válida: " + categoryName));
        }

        boolean isScheduled = requestDto.scheduledTime() != null;

        LocalDateTime creationTime = LocalDateTime.now();
        LocalDateTime publicationTime;

        if (isScheduled) {
            publicationTime = requestDto.scheduledTime(); // La publicación es en el futuro
        } else {
            publicationTime = creationTime; // La publicación es ahora
        }

        // Construir la entidad principal del hilo
        // Build the main entity of the thread
        ThreadClass newThread = ThreadClass.builder()
                .user(currentUser)
                .category(category)
                .createdAt(LocalDateTime.now())
                .isPublished(!isScheduled)
                .publishedAt(publicationTime)
                .scheduledTime(requestDto.scheduledTime())
                .likeCount(0)
                .commentCount(0)
                .saveCount(0)
                .viewCount(0)
                .build();

        // Construir las entidades de los posts y asociarlas al hilo
        // Build the post entities and associate them with the thread
        Post post1 = Post.builder()
                .content(requestDto.post1())
                .position(1)
                .thread(newThread)
                .build();

        Post post2 = Post.builder()
                .content(requestDto.post2())
                .position(2)
                .thread(newThread)
                .build();

        Post post3 = Post.builder()
                .content(requestDto.post3())
                .position(3)
                .thread(newThread)
                .build();

        // La relación es bidireccional, así que añadimos los posts a la lista del hilo
        // The relationship is bidirectional, so we add the posts to the thread list
        newThread.setPosts(List.of(post1, post2, post3));

        // Guardar el hilo en la base de datos
        // Gracias a `cascade = CascadeType.ALL`, al guardar el hilo, los posts se guardarán automáticamente.
        // Save the thread to the database
        // Thanks to `cascade = CascadeType.ALL`, when saving the thread, the messages will be saved automatically.
        ThreadClass savedThread = threadRepository.save(newThread);


        // Mapear la entidad guardada a un DTO de respuesta y devolverlo
        // Map the saved entity to a response DTO and return it
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
