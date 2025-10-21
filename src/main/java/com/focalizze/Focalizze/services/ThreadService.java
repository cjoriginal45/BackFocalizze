package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.ThreadRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;

public interface ThreadService {

    ThreadResponseDto createThread(ThreadRequestDto requestDto);
}
