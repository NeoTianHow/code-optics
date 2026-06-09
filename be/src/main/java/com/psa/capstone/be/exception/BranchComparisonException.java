package com.psa.capstone.be.exception;

public class BranchComparisonException extends RuntimeException {
    public BranchComparisonException(String message) {
        super(message);
    }

    public BranchComparisonException(String message, Throwable cause) {
        super(message, cause);
    }
}
