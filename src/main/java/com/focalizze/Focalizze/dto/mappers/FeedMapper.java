package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.StatsDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.ThreadClass;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FeedMapper {

    public FeedThreadDto toFeedThreadDto(ThreadClass thread) {
        ThreadResponseDto.UserDto authorDto = new ThreadResponseDto.UserDto(
                thread.getUser().getId(),
                thread.getUser().getUsername(),
                thread.getUser().getDisplayName()
        );

        List<String> postContents = thread.getPosts().stream()
                .sorted(Comparator.comparing(Post::getPosition))
                .map(Post::getContent)
                .collect(Collectors.toList());

        StatsDto statsDto = new StatsDto(
                thread.getLikeCount(),
                thread.getCommentCount(),
                thread.getSaveCount(),
                thread.getViewCount()
        );

        return new FeedThreadDto(
                thread.getId(),
                authorDto,
                thread.getCreatedAt(),
                postContents,
                statsDto,
                false,
                false
        );
    }
}
