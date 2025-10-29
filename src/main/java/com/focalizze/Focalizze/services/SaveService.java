package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

public interface SaveService {

    /**
     * Añade un guardado si no existe, o lo elimina si ya existe.
     * @param threadId El ID del hilo a guardar/quitar.
     * @param currentUser El usuario que realiza la acción.
     */
    void toggleSave(Long threadId, User currentUser);

}
