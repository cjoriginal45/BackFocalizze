package com.focalizze.Focalizze.services;

public interface PasswordResetService {
    void processForgotPassword(String email);
    boolean validateResetToken(String token);
    void resetPassword(String token, String newPassword);

}
