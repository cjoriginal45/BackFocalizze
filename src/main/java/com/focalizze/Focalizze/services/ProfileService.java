package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProfileService {
    ProfileResponseDto getProfile(String username);
    List<ThreadResponseDto> getThreadsForUser(String username, Pageable pageable);
}
