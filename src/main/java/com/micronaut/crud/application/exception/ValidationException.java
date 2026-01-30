package com.micronaut.crud.application.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails
 * Application layer exception
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void addError(String field, String message) {
        this.errors.put(field, message);
    }
}
