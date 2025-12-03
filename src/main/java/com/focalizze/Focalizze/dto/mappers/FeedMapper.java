package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.StatsDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FeedMapper {

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;

    public FeedThreadDto toFeedThreadDto(ThreadClass thread) {
        UserDto authorDto = new UserDto(
                thread.getUser().getId(),
                thread.getUser().getUsername(),
                thread.getUser().getDisplayName(),
                thread.getUser().getAvatarUrl(defaultAvatarUrl),
                thread.getUser().getCalculatedThreadCount(), false,
                thread.getUser().getFollowingCount(),
                thread.getUser().getFollowersCount(),
                false,
                thread.getUser().getRole().name()
        );

        List<String> postContents = thread.getPosts().stream()
                .sorted(Comparator.comparing(Post::getPosition))
                .map(Post::getContent)
                .collect(Collectors.toList());

        StatsDto statsDto = new StatsDto(
                thread.getLikeCount(),
                thread.getCommentCount(),
                thread.getViewCount(),
                thread.getSaveCount()

        );

        String categoryName = (thread.getCategory() != null) ? thread.getCategory().getName() : null;

        return new FeedThreadDto(
                thread.getId(),
                authorDto,
                thread.getCreatedAt(),
                postContents,
                statsDto,
                false, // isLiked (se llenará en el Enricher)
                false, // isSaved (se llenará en el Enricher)
                categoryName // <-- EL ARGUMENTO QUE FALTABA
        );
    }
}