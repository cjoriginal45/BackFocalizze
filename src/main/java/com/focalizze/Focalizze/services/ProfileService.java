package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ProfileUpdateRequestDto;
import com.focalizze.Focalizze.dto.ThreadResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProfileService {
    ProfileResponseDto getProfile(String username);
    List<ThreadResponseDto> getThreadsForUser(String username, Pageable pageable);

    ProfileResponseDto updateProfile(String username,ProfileUpdateRequestDto update);

    String updateAvatar(String username, MultipartFile file);
}
