package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import com.focalizze.Focalizze.models.User;

import java.time.LocalDateTime;

public interface ThreadService {

    ThreadResponseDto createThread(ThreadRequestDto requestDto);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime startOfDay);
}
