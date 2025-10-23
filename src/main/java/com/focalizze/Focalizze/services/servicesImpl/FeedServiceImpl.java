package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.mappers.FeedMapper;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final ThreadRepository threadRepository;
    private final FeedMapper feedMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedThreadDto> getFeed(Pageable pageable) {
        Page<ThreadClass> threadPage = threadRepository.findThreadsForFeed(pageable);
        return threadPage.map(feedMapper::toFeedThreadDto);
    }

}
