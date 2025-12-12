package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.HiddenContent;
import com.focalizze.Focalizze.models.ThreadClass;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.HiddenContentRepository;
import com.focalizze.Focalizze.repository.ThreadRepository;
import com.focalizze.Focalizze.services.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final HiddenContentRepository hiddenRepo;
    private final ThreadRepository threadRepo;

    @Override
    public void hideThread(Long threadId, String reasonType, User currentUser) {
        ThreadClass thread = threadRepo.findById(threadId).orElseThrow();

        HiddenContent hidden = HiddenContent.builder()
                .user(currentUser)
                .thread(thread)
                .reasonType(reasonType)
                .build();

        hiddenRepo.save(hidden);
    }

    @Override
    public Set<Long> getHiddenThreadIds(User currentUser) {
        return hiddenRepo.findHiddenThreadIdsByUser(currentUser);
    }
}