package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.FeedService;
import com.focalizze.Focalizze.utils.ThreadEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final ThreadRepository threadRepository;
    private final ThreadEnricher threadEnricher;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getFeed(Pageable pageable) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 1. Obtenemos la página de entidades.
        Page<ThreadClass> threadPage = threadRepository.findThreadsForFeed(pageable); // Con JOIN FETCH

        // 2. Obtenemos la lista de contenido de la página.
        List<ThreadClass> threadsOnPage = threadPage.getContent();

        // 3. Usamos el método optimizado 'enrichList' para enriquecer la lista.
        List<FeedThreadDto> enrichedDtoList = threadEnricher.enrichList(threadsOnPage, currentUser);

        // 4. Creamos una nueva instancia de 'Page' con el contenido enriquecido.
        return new PageImpl<>(enrichedDtoList, pageable, threadPage.getTotalElements());
    }
}
