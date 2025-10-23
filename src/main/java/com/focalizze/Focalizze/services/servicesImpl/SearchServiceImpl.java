package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.UserSearchDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.SearchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;

    public SearchServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
