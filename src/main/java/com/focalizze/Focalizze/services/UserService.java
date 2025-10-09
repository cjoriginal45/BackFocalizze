package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

import java.util.Optional;

public interface UserService {

    public Optional<User> findUserByUserName(String username);

    public Optional<User> findUserByEmail(String email);

    public Optional<User> findUserByUsernameOrEmail(String username, String email);

    public boolean validateEmail(String email);

    public boolean UserNameAvailable(String username);
}
