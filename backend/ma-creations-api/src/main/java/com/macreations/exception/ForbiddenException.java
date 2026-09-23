package com.macreations.exception;

import org.springframework.http.HttpStatus;

public final class ForbiddenException extends ApiException {

    public ForbiddenException(String code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }
}
