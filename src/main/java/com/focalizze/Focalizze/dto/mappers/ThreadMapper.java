package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.models.ThreadClass;
import org.springframework.stereotype.Component;
import com.focalizze.Focalizze.models.Post;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ThreadMapper {
    public ThreadResponseDto mapToResponseDto(ThreadClass thread) {
        ThreadResponseDto.UserDto authorDto = new ThreadResponseDto.UserDto(
                thread.getUser().getId(),
                thread.getUser().getUsername(),
                thread.getUser().getDisplayName()
        );

        List<String> postContents = thread.getPosts().stream()
                .map(Post::getContent)
                .collect(Collectors.toList());

        return new ThreadResponseDto(
                thread.getId(),
                authorDto,
                thread.getCategory().getName(), // Asumiendo que CategoryClass tiene un método getName()
                postContents,
                thread.getCreatedAt()
        );
    }

    public List<ThreadResponseDto> toDtoList(List<ThreadClass> threads) {
        // MEJORA 1: Manejo de nulos para robustez.
        if (threads == null) {
            return Collections.emptyList();
        }

        // MEJORA 2: Usamos una referencia a método (this::toDto) para mayor claridad.
        return threads.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
}
