package com.focalizze.Focalizze.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FeedThreadDto(
        Long id,
        UserDto user,
        LocalDateTime publicationDate,
        List<String> posts,
        StatsDto stats,
        boolean isLiked,
        boolean isSaved,
        String categoryName, // <-- El campo que vamos a poblar
        List<String> images
) {
    // Este es el método que estabas usando. Lo reemplazaremos por el constructor directo.
    public FeedThreadDto withInteractionStatus(boolean newIsLiked, boolean newIsSaved) {
        return new FeedThreadDto(
                this.id, this.user, this.publicationDate, this.posts,
                this.stats, newIsLiked, newIsSaved, this.categoryName, this.images // <-- Fíjate que arrastra el categoryName existente (que era null)
        );
    }

}