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
        boolean isSaved
) {

    /**
     * Devuelve una NUEVA instancia de FeedThreadDto con los valores de isLiked y isSaved actualizados.
     * Este es un "copy constructor" o "wither method" que respeta la inmutabilidad de los records.
     *
     * @param newIsLiked El nuevo valor para el campo isLiked.
     * @param newIsSaved El nuevo valor para el campo isSaved.
     * @return Una nueva instancia de FeedThreadDto con los valores actualizados.
     */
    public FeedThreadDto withInteractionStatus(boolean newIsLiked, boolean newIsSaved) {
        // Llama al constructor del record, pasando todos los valores existentes
        // y reemplazando solo los que han cambiado.
        return new FeedThreadDto(
                this.id,
                this.user,
                this.publicationDate,
                this.posts,
                this.stats,
                newIsLiked,
                newIsSaved
        );
    }

}