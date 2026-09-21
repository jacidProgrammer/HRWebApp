package dev.jacid.hrApplication.domain.exception;

import java.util.UUID;

public class EmployeeNotFoundException extends DomainException {

    private EmployeeNotFoundException(String message) {
        super(message);
    }

    public static EmployeeNotFoundException withId(UUID id) {
        return new EmployeeNotFoundException("Employee '" + id + "' not found");
    }

    public static EmployeeNotFoundException linkedTo(String username) {
        return new EmployeeNotFoundException("No employee record is linked to user '" + username + "'");
    }
}
