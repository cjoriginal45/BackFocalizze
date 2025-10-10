package com.focalizze.Focalizze.exceptions;

public class EmailNotFoundException extends RuntimeException{

    public EmailNotFoundException(String message) {
        super("Email no encontrado");
    }
}
