package com.psa.capstone.be.exception;

public class WebhookProcessingException extends RuntimeException {
    public WebhookProcessingException(String message) {
        super(message);
    }

    public WebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
