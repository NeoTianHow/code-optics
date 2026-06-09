package com.psa.capstone.be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return buildErrorResponse(
                "Validation failed: " + String.join(", ", errors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidWebhookException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidWebhook(InvalidWebhookException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WebhookProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookProcessing(WebhookProcessingException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CodeReviewException.class)
    public ResponseEntity<Map<String, Object>> handleCodeReviewInitiation(CodeReviewException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientRequestError(WebClientRequestException ex) {
        logger.error("Network error when contacting GitLab: {}", ex.getMessage(), ex);
        return buildErrorResponse(
                "Network error when contacting GitLab: " + ex.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(GitLabNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(GitLabNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GitLabException.class)
    public ResponseEntity<Map<String, Object>> handleGenericGitLabError(GitLabException ex) {
        logger.error("GitLab API error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", ZonedDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

}