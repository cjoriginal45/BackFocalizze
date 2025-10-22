package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.StatsDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FeedMapper {

    public FeedThreadDto toFeedThreadDto(ThreadClass thread) {
        // 1. Mapeamos la información del autor (reutilizando la lógica)
        // We map the author's information (reusing logic)
        ThreadResponseDto.UserDto authorDto = mapUserToUserDto(thread.getUser());

        // 2. Mapeamos el contenido de los posts en orden
        // We map the content of the posts in order
        List<String> postContents = mapPostsToStrings(thread.getPosts());

        // 3. Mapeamos las estadísticas directamente desde los contadores
        // We map statistics directly from the counters
        StatsDto statsDto = new StatsDto(
                thread.getLikeCount() != null ? thread.getLikeCount() : 0,
                thread.getCommentCount() != null ? thread.getCommentCount() : 0,
                thread.getSaveCount() != null ? thread.getSaveCount() : 0,
                thread.getViewCount() != null ? thread.getViewCount() : 0
        );

        // 4. Creamos el DTO principal con valores por defecto para isLiked/isSaved
        // We create the main DTO with default values ​​for isLiked/isSaved
        return new FeedThreadDto(
                thread.getId(),
                authorDto,
                thread.getCreatedAt(),
                postContents,
                statsDto,
                false, // Valor por defecto, se sobrescribirá en el servicio / Default value, will be overwritten in the service
                false  // Valor por defecto, se sobrescribirá en el servicio / Default value, will be overwritten in the service
        );
    }

    // Métodos de ayuda privados para mantener el código limpio
    // Private helper methods to keep your code clean
    private ThreadResponseDto.UserDto mapUserToUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new ThreadResponseDto.UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName()
        );
    }

    private List<String> mapPostsToStrings(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }
        return posts.stream()
                .sorted(Comparator.comparing(Post::getPosition))
                .map(Post::getContent)
                .collect(Collectors.toList());
    }
}
