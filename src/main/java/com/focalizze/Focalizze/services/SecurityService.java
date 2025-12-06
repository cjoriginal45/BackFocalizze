package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.models.User;

public interface SecurityService {
    void toggleTwoFactor(boolean enabled, User currentUser);
    void logoutAllDevices(User currentUser);
    boolean validatePassword(String rawPassword, User currentUser);
}
