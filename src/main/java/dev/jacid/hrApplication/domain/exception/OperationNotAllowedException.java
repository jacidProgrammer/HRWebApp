package dev.jacid.hrApplication.domain.exception;

/** The caller is authenticated but the business rules do not allow this operation for them. */
public class OperationNotAllowedException extends DomainException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
