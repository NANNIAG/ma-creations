package com.macreations.exception;

import org.springframework.http.HttpStatus;

public final class NotFoundException extends ApiException {

    public NotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
