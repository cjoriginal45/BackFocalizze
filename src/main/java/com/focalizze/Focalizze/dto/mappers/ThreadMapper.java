package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.StatsDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.ThreadClass;
import org.springframework.stereotype.Component;
import com.focalizze.Focalizze.models.Post;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ThreadMapper {
    public ThreadResponseDto mapToResponseDto(ThreadClass thread) {
        UserDto authorDto = new UserDto(thread.getUser().getId(),
                thread.getUser().getUsername(),
                thread.getUser().getDisplayName(),
                thread.getUser().getAvatarUrl()
        );

        StatsDto statsDto = new StatsDto(
                thread.getLikeCount(),
                thread.getCommentCount(),
                thread.getViewCount(),
                thread.getSaveCount()
        );

        List<String> postContents = thread.getPosts().stream()
                .map(Post::getContent)
                .collect(Collectors.toList());
        String categoryName = (thread.getCategory() != null) ? thread.getCategory().getName() : null;

        return new ThreadResponseDto(
                thread.getId(),
                authorDto,
                categoryName,
                postContents,
                thread.getCreatedAt(),
                statsDto
        );
    }

    public List<ThreadResponseDto> toDtoList(List<ThreadClass> threads) {
        if (threads == null) {
            return Collections.emptyList();
        }
        return threads.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
}
