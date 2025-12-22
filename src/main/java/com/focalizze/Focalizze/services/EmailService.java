package com.focalizze.Focalizze.services;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
    // Método para 2FA
    void sendTwoFactorCode(String to, String code);
}
