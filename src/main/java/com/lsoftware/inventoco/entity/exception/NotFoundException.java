package com.lsoftware.inventoco.entity.exception;

/** Something the request names does not exist for the current user (404). */
public class NotFoundException extends DomainException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
