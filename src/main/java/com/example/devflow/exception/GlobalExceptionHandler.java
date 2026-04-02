package com.example.devflow.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error,
                        String message, HttpServletRequest request) {
                ErrorResponse body = ErrorResponse.builder()
                                .status(status.value())
                                .error(error)
                                .message(message)
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .build();
                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> e.getField() + " " + e.getDefaultMessage())
                                .findFirst()
                                .orElse("Validation failed");
                return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(
                        InvalidCredentialsException ex, HttpServletRequest request) {
                return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
        }

        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(
                        EntityNotFoundException ex, HttpServletRequest request) {
                return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
        }

        @ExceptionHandler(InvalidStateTransitionException.class)
        public ResponseEntity<ErrorResponse> handleStateTransition(
                        InvalidStateTransitionException ex, HttpServletRequest request) {
                return buildResponse(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", ex.getMessage(), request);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleMalformedJson(
                        HttpMessageNotReadableException ex, HttpServletRequest request) {
                return buildResponse(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                                "Request body is malformed or missing", request);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(
                        Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                                "An unexpected error occurred", request);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
                return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                                ex.getMethod() + " method is not supported for this endpoint", request);
        }
}