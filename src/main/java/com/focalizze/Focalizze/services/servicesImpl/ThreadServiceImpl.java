package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.ThreadService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ThreadServiceImpl implements ThreadService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadMapper threadMapper;

    private static final int DAILY_THREAD_LIMIT = 3;

    public ThreadServiceImpl(ThreadRepository threadRepository,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             ThreadMapper threadMapper) {
        this.threadRepository = threadRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.threadMapper = threadMapper;
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

    @Override
    @Transactional
    public long countByUserAndCreatedAtAfter(User user, LocalDateTime startOfDay) {
        // 1. Obtener el usuario autenticado
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Usuario no autenticado, no se puede crear el hilo."));

        // 2. Calcular cuántos hilos ha creado el usuario hoy
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay(); // Obtiene la fecha de hoy a las 00:00:00
        long threadsCreatedToday = threadRepository.countByUserAndCreatedAtAfter(currentUser, startOfToday);

        // 3. Aplicar la regla de negocio
        if (threadsCreatedToday >= DAILY_THREAD_LIMIT) {
            // Si el usuario ya ha alcanzado o superado el límite, lanzamos una excepción.
            //            // Esta excepción debería ser capturada por un @ControllerAdvice para devolver un error 429 Too Many Requests.
            throw new IllegalStateException("Límite diario de hilos alcanzado. No se pueden crear más hilos hoy.");
        }

        return threadsCreatedToday;
    }


}
