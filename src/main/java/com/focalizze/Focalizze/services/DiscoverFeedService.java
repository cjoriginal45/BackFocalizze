package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DiscoverFeedService {
    Page<DiscoverItemDto> getDiscoverFeed(User currentUser, Pageable pageable);
}
