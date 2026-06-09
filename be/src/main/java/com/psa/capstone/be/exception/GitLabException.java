package com.psa.capstone.be.exception;

public class GitLabException extends RuntimeException {
    public GitLabException(String message) {
        super(message);
    }

    public GitLabException(String message, Throwable cause) {
        super(message, cause);
    }
}