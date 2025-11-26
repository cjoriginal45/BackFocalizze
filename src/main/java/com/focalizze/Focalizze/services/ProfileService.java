package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.FeedThreadDto;
import com.focalizze.Focalizze.dto.ProfileResponseDto;
import com.focalizze.Focalizze.dto.ProfileUpdateRequestDto;
import com.focalizze.Focalizze.dto.UserProfileDownloadDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    ProfileResponseDto getProfile(String username);
    Page<FeedThreadDto> getThreadsForUser(String username, Pageable pageable);
    ProfileResponseDto updateProfile(String username,ProfileUpdateRequestDto update);
    String updateAvatar(String username, MultipartFile file);
    UserProfileDownloadDto getProfileForDownload(String username);

}
