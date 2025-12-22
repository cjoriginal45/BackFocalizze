package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.StatsDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.ThreadImage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Mapper for converting Thread entities into FeedThreadDto.
 * Specifically tailored for feed display requirements.
 * <p>
 * Mapper para convertir entidades Hilo en FeedThreadDto.
 * Específicamente adaptado para los requisitos de visualización del feed.
 */
@Component
public class FeedMapper {

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;

    /**
     * Converts a Thread entity to a DTO suitable for feeds.
     * Note: Interaction flags (isLiked, isSaved) are set to false by default and enriched later.
     * <p>
     * Convierte una entidad Hilo a un DTO adecuado para feeds.
     * Nota: Las banderas de interacción (isLiked, isSaved) se establecen en falso por defecto y se enriquecen más tarde.
     *
     * @param thread The thread entity. / La entidad hilo.
     * @return The DTO. / El DTO.
     */
    public FeedThreadDto toFeedThreadDto(ThreadClass thread) {
        UserDto authorDto = new UserDto(
                thread.getUser().getId(),
                thread.getUser().getUsername(),
                thread.getUser().getDisplayName(),
                thread.getUser().getAvatarUrl(defaultAvatarUrl),
                thread.getUser().getCalculatedThreadCount(),
                false,
                thread.getUser().getFollowingCount(),
                thread.getUser().getFollowersCount(),
                false,
                thread.getUser().getRole().name(),
                thread.getUser().isTwoFactorEnabled(),
                thread.getUser().getBackgroundType(),
                thread.getUser().getBackgroundValue()
        );

        List<String> postContents = thread.getPosts().stream()
                .sorted(Comparator.comparing(Post::getPosition))
                .map(Post::getContent)
                .toList();

        StatsDto statsDto = new StatsDto(
                thread.getLikeCount(),
                thread.getCommentCount(),
                thread.getViewCount(),
                thread.getSaveCount()

        );

        String categoryName = (thread.getCategory() != null) ? thread.getCategory().getName() : null;

        List<String> imageUrls = thread.getImages() != null
                ? thread.getImages().stream()
                .map(ThreadImage::getImageUrl)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new FeedThreadDto(
                thread.getId(),
                authorDto,
                thread.getCreatedAt(),
                postContents,
                statsDto,
                false,
                false,
                categoryName,
                imageUrls
        );
    }
}