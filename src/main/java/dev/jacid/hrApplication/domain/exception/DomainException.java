package dev.jacid.hrApplication.domain.exception;

/** Base class for business rule violations; each subclass maps to one HTTP status in the web adapter. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
