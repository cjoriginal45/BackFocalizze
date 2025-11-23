package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.DiscoverItemDto;
import com.focalizze.Focalizze.models.User;

import java.util.List;

public interface RecommendationService {
    List<DiscoverItemDto> getRecommendations(User currentUser, int limit);
}
