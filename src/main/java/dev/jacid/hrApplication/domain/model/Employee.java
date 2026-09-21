package dev.jacid.hrApplication.domain.model;

import java.util.ArrayList;
import java.util.List;

import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;

/**
 * An employee of the company. {@code salary} and {@code address} are sensitive and are only
 * visible to managers and to the employee themselves.
 */
public record Employee(Long id,
                       String name,
                       String department,
                       String role,
                       String email,
                       Double salary,
                       String address) {

    /** Copy of this employee with the sensitive fields (salary, address) removed. */
    public Employee withoutSensitiveData() {
        return new Employee(id, name, department, role, email, null, null);
    }

    /** @throws InvalidEmployeeDataException if a mandatory field is missing */
    public Employee requireComplete() {
        List<String> missing = new ArrayList<>();
        if (isBlank(name)) missing.add("name");
        if (isBlank(department)) missing.add("department");
        if (isBlank(role)) missing.add("role");
        if (isBlank(email)) missing.add("email");
        if (salary == null) missing.add("salary");
        if (isBlank(address)) missing.add("address");
        if (!missing.isEmpty()) {
            throw new InvalidEmployeeDataException("Missing required fields: " + String.join(", ", missing));
        }
        return this;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
