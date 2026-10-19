package com.devrenanrodrigues.goalmanagementapi.common.exception;

public class AccessForbiddenException extends RuntimeException {
    public AccessForbiddenException(String message) {
        super(message);
    }
}
