package com.focalizze.Focalizze.services;


import com.focalizze.Focalizze.dto.FeedThreadDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedService {
    Page<FeedThreadDto> getFeed(Pageable pageable);
}
