package com.focalizze.Focalizze.exceptions;

/**
 * Exception thrown when an email lookup fails.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
