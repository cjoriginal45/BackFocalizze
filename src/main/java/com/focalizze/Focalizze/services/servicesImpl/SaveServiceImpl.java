package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.SavedThreads;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.SaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveServiceImpl implements SaveService {

    private final ThreadRepository threadRepository;
    private final SavedThreadRepository savedThreadRepository;

    @Override
    @Transactional
    public void toggleSave(Long threadId, User currentUser) {
        // 1. Validar que el hilo exista.
        // 1. Validate that the thread exists.
        ThreadClass thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread no encontrado con id: " + threadId));

        // 2. Comprobar si el usuario ya ha guardado este hilo.
        // 2. Check if the user has already saved this thread.
        Optional<SavedThreads> existingSave = savedThreadRepository.findByUserAndThread(currentUser, thread);

        if (existingSave.isPresent()) {
            // 3. Si ya está guardado, lo eliminamos.
            // 3. If it is already saved, we delete it.
            savedThreadRepository.delete(existingSave.get());
            thread.setSaveCount(thread.getSaveCount() - 1);
        } else {
            // 4. Si no está guardado, creamos una nueva entrada.
            // 4. If it is not saved, we create a new entry.
            SavedThreads newSave = SavedThreads.builder()
                    .user(currentUser)
                    .thread(thread)
                    .build();
            savedThreadRepository.save(newSave);
            thread.setSaveCount(thread.getSaveCount() + 1);
        }

        // 5. Guardamos la entidad del hilo con el contador actualizado.
        // 5. We save the thread entity with the updated counter.
        threadRepository.save(thread);
    }

}
