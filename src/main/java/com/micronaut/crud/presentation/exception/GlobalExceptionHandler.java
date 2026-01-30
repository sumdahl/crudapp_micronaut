package com.micronaut.crud.presentation.exception;

import com.micronaut.crud.application.exception.ValidationException;
import com.micronaut.crud.domain.exception.DuplicateResourceException;
import com.micronaut.crud.domain.exception.ResourceNotFoundException;
import com.micronaut.crud.presentation.dto.ErrorResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler for the application
 * Catches all exceptions and returns appropriate HTTP responses
 */
@Produces
@Singleton
@Requires(classes = { Exception.class, ExceptionHandler.class })
public class GlobalExceptionHandler implements ExceptionHandler<Exception, HttpResponse<ErrorResponse>> {

    @Override
    @SuppressWarnings("rawtypes")
    public HttpResponse<ErrorResponse> handle(HttpRequest request, Exception exception) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setPath(request.getPath());

        // Handle specific exceptions
        if (exception instanceof ResourceNotFoundException) {
            return handleResourceNotFoundException(request, (ResourceNotFoundException) exception, errorResponse);
        } else if (exception instanceof DuplicateResourceException) {
            return handleDuplicateResourceException(request, (DuplicateResourceException) exception, errorResponse);
        } else if (exception instanceof ValidationException) {
            return handleValidationException(request, (ValidationException) exception, errorResponse);
        } else if (exception instanceof ConstraintViolationException) {
            return handleConstraintViolationException(request, (ConstraintViolationException) exception, errorResponse);
        } else if (exception instanceof IllegalArgumentException) {
            return handleIllegalArgumentException(request, exception, errorResponse);
        } else {
            return handleGenericException(request, exception, errorResponse);
        }
    }

    private HttpResponse<ErrorResponse> handleResourceNotFoundException(
            HttpRequest request,
            ResourceNotFoundException exception,
            ErrorResponse errorResponse) {
        errorResponse.setStatus(HttpStatus.NOT_FOUND.getCode());
        errorResponse.setError("Not Found");
        errorResponse.setMessage(exception.getMessage());
        return HttpResponse.notFound(errorResponse);
    }

    private HttpResponse<ErrorResponse> handleDuplicateResourceException(
            HttpRequest request,
            DuplicateResourceException exception,
            ErrorResponse errorResponse) {
        errorResponse.setStatus(HttpStatus.CONFLICT.getCode());
        errorResponse.setError("Conflict");
        errorResponse.setMessage(exception.getMessage());
        return HttpResponse.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    private HttpResponse<ErrorResponse> handleValidationException(
            HttpRequest request,
            ValidationException exception,
            ErrorResponse errorResponse) {
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.getCode());
        errorResponse.setError("Bad Request");
        errorResponse.setMessage(exception.getMessage());
        if (exception.getErrors() != null && !exception.getErrors().isEmpty()) {
            errorResponse.addValidationErrors(exception.getErrors());
        }
        return HttpResponse.badRequest(errorResponse);
    }

    private HttpResponse<ErrorResponse> handleConstraintViolationException(
            HttpRequest request,
            ConstraintViolationException exception,
            ErrorResponse errorResponse) {
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.getCode());
        errorResponse.setError("Bad Request");
        errorResponse.setMessage("Validation failed");

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errorResponse.addValidationError(fieldName, message);
        }

        return HttpResponse.badRequest(errorResponse);
    }

    private HttpResponse<ErrorResponse> handleIllegalArgumentException(
            HttpRequest request,
            Exception exception,
            ErrorResponse errorResponse) {
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.getCode());
        errorResponse.setError("Bad Request");
        errorResponse.setMessage(exception.getMessage());
        return HttpResponse.badRequest(errorResponse);
    }

    private HttpResponse<ErrorResponse> handleGenericException(
            HttpRequest request,
            Exception exception,
            ErrorResponse errorResponse) {
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        errorResponse.setError("Internal Server Error");
        errorResponse.setMessage("An unexpected error occurred");

        // Log the full exception for debugging
        exception.printStackTrace();

        return HttpResponse.serverError(errorResponse);
    }
}
