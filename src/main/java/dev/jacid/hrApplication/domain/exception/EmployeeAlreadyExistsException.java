package dev.jacid.hrApplication.domain.exception;

public class EmployeeAlreadyExistsException extends DomainException {

    public EmployeeAlreadyExistsException(String username) {
        super("An employee with username '" + username + "' already exists");
    }
}
