package dev.jacid.hrApplication.application.port.in;

import java.util.List;

import dev.jacid.hrApplication.domain.model.Employee;

public interface EmployeesUseCases {
    List<Employee> getAllEmployees();
    Employee getEmployeeByName(String name);
    Employee createEmployee(Employee employee);
    /** Updates the employee identified by {@code name}; the name itself cannot be changed. */
    Employee updateEmployee(String name, Employee changes);
    void deleteEmployeeByName(String name);
}
