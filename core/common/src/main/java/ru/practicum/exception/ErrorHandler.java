package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException e) {
        log.error("404 Not Found: {}", e.getMessage());
        return new ApiError(
                HttpStatus.NOT_FOUND.name(),
                "The required object was not found.",
                e.getMessage(),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(ConflictException e) {
        log.error("409 Conflict: {}", e.getMessage());
        return new ApiError(
                HttpStatus.CONFLICT.name(),
                "For the requested operation the conditions are not met.",
                e.getMessage(),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(BadRequestException e) {
        log.error("400 Bad Request: {}", e.getMessage());
        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                e.getMessage(),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("400 Bad Request: {}", e.getMessage());
        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                e.getMessage(),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("400 Bad Request - Validation error: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = "Validation error: " + errors.toString();

        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                message,
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("400 Bad Request - Missing parameter: {}", e.getMessage());
        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                String.format("Missing required parameter: %s", e.getParameterName()),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("400 Bad Request - Type mismatch: {}", e.getMessage());
        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                String.format("Invalid value for parameter '%s': %s", e.getName(), e.getValue()),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("400 Bad Request - Message not readable: {}", e.getMessage());
        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                "Required request body is missing or invalid",
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("409 Conflict - Data integrity violation: {}", e.getMessage());
        String message = e.getMostSpecificCause().getMessage();
        String reason = "Integrity constraint has been violated.";

        if (message.contains("users_email_key") || message.contains("uq_email")) {
            message = "User with this email already exists";
        } else if (message.contains("categories_name_key") || message.contains("uq_category_name")) {
            message = "Category with this name already exists";
        } else if (message.contains("uq_request") || message.contains("requests_event_id_requester_id_key")) {
            message = "Request already exists";
        }

        return new ApiError(
                HttpStatus.CONFLICT.name(),
                reason,
                message,
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception e) {
        log.error("500 Internal Server Error: {}", e.getMessage(), e);
        return new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                "Internal server error.",
                e.getMessage(),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolationException(jakarta.validation.ConstraintViolationException e) {
        log.error("400 Bad Request - Constraint violation: {}", e.getMessage());
        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                e.getMessage(),
                LocalDateTime.now().format(FORMATTER),
                Collections.emptyList()
        );
    }
}