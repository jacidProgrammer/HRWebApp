package dev.jacid.hrApplication.domain.exception;

/** A request parameter or query is invalid (e.g. an unknown filter value or an out-of-range number). */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
