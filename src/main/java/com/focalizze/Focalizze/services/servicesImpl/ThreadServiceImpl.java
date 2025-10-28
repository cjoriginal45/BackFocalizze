package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.ThreadService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ThreadServiceImpl implements ThreadService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadMapper threadMapper;
    private final FeedMapper feedMapper;
    private final SavedThreadRepository savedThreadRepository;

    public ThreadServiceImpl(ThreadRepository threadRepository,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             ThreadMapper threadMapper, FeedMapper feedMapper, SavedThreadRepository savedThreadRepository) {
        this.threadRepository = threadRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.threadMapper = threadMapper;
        this.feedMapper = feedMapper;
        this.savedThreadRepository = savedThreadRepository;
    }


    //Logica de negocio para crear un hilo
    //Business logic to create a thread
    @Override
    @Transactional
    public ThreadResponseDto createThread(ThreadRequestDto requestDto) {
        // 1. Obtener el usuario autenticado desde el contexto de seguridad
        // 1. Get the authenticated user from the security context
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado, no se puede crear el hilo."));

        // 2. Buscar la categoría en la base de datos
        // 2. Search the category in the database
        CategoryClass category = categoryRepository.findByName(requestDto.category())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no válida: " + requestDto.category()));

        // 3. Construir la entidad principal del hilo
        // 3. Build the main entity of the thread
        ThreadClass newThread = ThreadClass.builder()
                .user(currentUser)
                .category(category)
                .createdAt(LocalDateTime.now())
                .isPublished(true) // false si es un hilo programado
                .likeCount(0)
                .commentCount(0)
                .saveCount(0)
                .viewCount(0)
                .build();

        // 4. Construir las entidades de los posts y asociarlas al hilo
        // 4. Build the post entities and associate them with the thread
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

        // 5. Guardar el hilo en la base de datos
        // Gracias a `cascade = CascadeType.ALL`, al guardar el hilo, los posts se guardarán automáticamente.
        // 5. Save the thread to the database
        // Thanks to `cascade = CascadeType.ALL`, when saving the thread, the messages will be saved automatically.
        ThreadClass savedThread = threadRepository.save(newThread);


        // 6. Mapear la entidad guardada a un DTO de respuesta y devolverlo
        // 6. Map the saved entity to a response DTO and return it
        return threadMapper.mapToResponseDto(savedThread);
    }

    // Método para obtener detalles e incrementar vistas
    // Method to get details and increment views
    @Override
    @Transactional
    public FeedThreadDto getThreadByIdAndIncrementView(Long threadId) {
        // 1. Buscamos el hilo con toda su información
        // 1. We look for the thread with all its information
        ThreadClass thread = threadRepository.findThreadDetailsById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado con id: " + threadId));

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
                dto.stats(), isLiked, isSaved
        );
    }
}
