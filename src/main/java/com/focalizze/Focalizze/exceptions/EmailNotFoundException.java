package com.focalizze.Focalizze.exceptions;

/**
 * Exception thrown when trying to register with existing credentials.
 */
public class EmailNotFoundException extends RuntimeException{
    public EmailNotFoundException(String message) {
        super("Email no encontrado");
    }
}
