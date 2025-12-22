package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.BlockedUserDto;

import java.util.List;

public interface BlockService {

    boolean toggleBlock(String usernameToToggle);

    List<BlockedUserDto> getBlockedUsers();
}
