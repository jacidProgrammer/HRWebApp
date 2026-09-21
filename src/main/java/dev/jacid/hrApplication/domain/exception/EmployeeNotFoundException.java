package dev.jacid.hrApplication.domain.exception;

public class EmployeeNotFoundException extends DomainException {

    public EmployeeNotFoundException(String name) {
        super("Employee '" + name + "' not found");
    }
}
