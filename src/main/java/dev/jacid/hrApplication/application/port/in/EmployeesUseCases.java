package dev.jacid.hrApplication.application.port.in;

import java.util.List;
import java.util.UUID;

import dev.jacid.hrApplication.domain.model.Employee;

public interface EmployeesUseCases {
    /** Every employee; salary and address only for managers and for the caller's own record. */
    List<Employee> getAllEmployees();
    /** The employee linked to the caller's username. */
    Employee getCurrentEmployee();
    Employee getEmployee(UUID id);
    Employee createEmployee(Employee employee);
    /** Updates the employee identified by {@code id}; the username never changes. */
    Employee updateEmployee(UUID id, Employee changes);
    /** Deletes the employee, the feedback about them, and removes them as author of the feedback they wrote. */
    void deleteEmployee(UUID id);
}
