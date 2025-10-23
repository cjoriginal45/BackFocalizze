package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.UserSearchDto;

import java.util.List;

public interface SearchService {

    List<UserSearchDto> searchUsersByPrefix(String prefix);
}
