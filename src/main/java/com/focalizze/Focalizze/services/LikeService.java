package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

public interface LikeService {
    /**
     * Añade un like si no existe, o lo elimina si ya existe.
     * @param threadId El ID del hilo a dar/quitar like.
     * @param currentUser El usuario que realiza la acción.
     *
     * Adds a like if it doesn't exist, or removes it if it already exists.
     * @param threadId The ID of the thread to like/unlike.
     * @param currentUser The user performing the action.
     */
    void toggleLike(Long threadId, User currentUser);
}
