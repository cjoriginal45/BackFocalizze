package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.dto.mappers.ThreadMapper;
import com.focalizze.Focalizze.models.CategoryClass;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.CategoryRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.SearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadRepository threadRepository;
    private final ThreadMapper threadMapper;

    public SearchServiceImpl(UserRepository userRepository, CategoryRepository categoryRepository,
                             ThreadRepository threadRepository, ThreadMapper threadMapper) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.threadRepository = threadRepository;
        this.threadMapper = threadMapper;
    }

    @Override
    public List<UserSearchDto> searchUsersByPrefix(String prefix) {
        // La búsqueda no debería incluir el '@'
        String cleanPrefix = prefix.startsWith("@") ? prefix.substring(1) : prefix;

        // Devuelve una lista vacía si la búsqueda está vacía
        if (cleanPrefix.isBlank()) {
            return List.of();
        }

        List<User> users = userRepository.findTop5ByUsernameStartingWithIgnoreCase(cleanPrefix);

        // Mapea a DTO
        return users.stream()
                .map(user -> new UserSearchDto(user.getUsername(), user.getDisplayName(), user.getAvatarUrl()))
                .collect(Collectors.toList());
    }



    @Override
    @Transactional(readOnly = true)
    public List<ThreadResponseDto> searchContent(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Estrategia de búsqueda:
        // 1. Intentar buscar por nombre de categoría exacto.
        Optional<CategoryClass> category = categoryRepository.findByName(query);

        List<ThreadClass> foundThreads;

        if (category.isPresent()) {
            // Si encontramos una categoría, devolvemos todos los hilos de esa categoría.
            System.out.println("Buscando hilos por categoría: " + query);
            foundThreads = threadRepository.findByCategory(category.get());
        } else {
            // Si no, realizamos una búsqueda de texto completo en el contenido de los posts.
            System.out.println("Buscando hilos por contenido de post: " + query);
            foundThreads = threadRepository.findByPostContentContainingIgnoreCase(query);
        }

        // Mapeamos los resultados al DTO de respuesta.
        return threadMapper.toDtoList(foundThreads);
    }

}
