package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.SavedThreads;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.SavedThreadRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.SaveService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveServiceImpl implements SaveService {

    private final ThreadRepository threadRepository;
    private final SavedThreadRepository savedThreadRepository;
    private final ThreadEnricher threadEnricher;

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

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getSavedThreadsForCurrentUser(Pageable pageable) {
        // 1. Obtenemos al usuario autenticado.
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Buscamos en la tabla 'saved_threads_tbl' para obtener la página correcta.
        //    Esto nos da una página de entidades 'SavedThreads'.
        Page<SavedThreads> savedThreadsPage = savedThreadRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);

        // 3. Extraemos las entidades 'ThreadClass' de las entidades 'SavedThreads'.
        List<ThreadClass> threads = savedThreadsPage.getContent().stream()
                .map(SavedThreads::getThread)
                .toList();

        // 4. Enriquecemos esta lista de hilos con la información de interacción.
        List<FeedThreadDto> enrichedDtoList = threadEnricher.enrichList(threads, currentUser);

        // 5. Creamos y devolvemos un nuevo objeto Page con el contenido enriquecido.
        return new PageImpl<>(enrichedDtoList, pageable, savedThreadsPage.getTotalElements());
    }

}
