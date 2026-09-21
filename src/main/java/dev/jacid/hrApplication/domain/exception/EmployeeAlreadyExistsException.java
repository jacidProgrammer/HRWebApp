package dev.jacid.hrApplication.domain.exception;

public class EmployeeAlreadyExistsException extends DomainException {

    public EmployeeAlreadyExistsException(String name) {
        super("Employee '" + name + "' already exists");
    }
}
