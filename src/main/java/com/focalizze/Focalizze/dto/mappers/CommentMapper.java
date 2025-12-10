package com.focalizze.Focalizze.dto.mappers;

import com.focalizze.Focalizze.dto.CommentResponseDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.CommentClass;
import com.focalizze.Focalizze.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CommentMapper {

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;
    /**
     * Convierte una entidad CommentClass a un DTO de respuesta para la API.
     * @param comment La entidad del comentario obtenida de la base de datos.
     * @return Un DTO con la información formateada para el frontend.
     *
     * * Converts a CommentClass entity to a response DTO for the API.
     * * @param comment The comment entity retrieved from the database.
     * * @return A DTO with the information formatted for the frontend.
     */
    public CommentResponseDto toCommentResponseDto(CommentClass comment) {
        if (comment == null) {
            return null;
        }

        // Mapeamos la información del autor del comentario usando un método de ayuda.
        // We map the comment author information using a helper method.
        UserDto authorDto = mapUserToUserDto(comment.getUser());

        List<CommentResponseDto> replyDtos = comment.getReplies().stream()
                .map(this::toCommentResponseDto)
                .sorted(Comparator.comparing(CommentResponseDto::createdAt))
                .toList();

        // Creamos y devolvemos el DTO de respuesta del comentario.
        // We create and return the comment response DTO.
        return new CommentResponseDto(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                authorDto,
                replyDtos
        );
    }

    /**
     * Método de ayuda privado para convertir una entidad User a su correspondiente UserDto.
     * Esto mantiene el código principal limpio y es reutilizable.
     * @param user La entidad del usuario.
     * @return Un DTO con los datos públicos del usuario.
     *
     * * Private helper method to convert a User entity to its corresponding UserDTO.
     * * This keeps the main code clean and reusable.
     * * @param user The user entity.
     * * @return A DTO with the user's public data.
     */
    private UserDto mapUserToUserDto(User user) {
        if (user == null) {
            // Devuelve nulo o un DTO por defecto si el autor no existe,
            // aunque en un sistema bien diseñado, esto no debería ocurrir.
            // Returns null or a default DTO if the author does not exist,
            // although in a well-designed system, this should not happen.
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(defaultAvatarUrl),
                user.getCalculatedThreadCount(),
                false,
                user.getFollowingCount(),
                user.getFollowersCount(),
                false,
                user.getRole().name(),
                user.isTwoFactorEnabled(),
                user.getBackgroundType(),
                user.getBackgroundValue()
        );
    }
}
