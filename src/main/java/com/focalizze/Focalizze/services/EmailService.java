package com.focalizze.Focalizze.services;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);

}
