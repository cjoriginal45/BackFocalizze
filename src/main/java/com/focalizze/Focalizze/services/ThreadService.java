package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.*;
import com.focalizze.Focalizze.models.User;

import java.time.LocalDateTime;

public interface ThreadService {

    ThreadResponseDto createThread(ThreadRequestDto requestDto);

    FeedThreadDto getThreadByIdAndIncrementView(Long threadId);

    void deleteThread(Long threadId, User currentUser);
    ThreadResponseDto updateThread(Long threadId, ThreadUpdateRequestDto updateDto, User currentUser);

    int getThreadsAvailableToday(User user);
}