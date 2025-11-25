package dev.jacid.hrApplication.application.port.in;

import java.util.List;

import dev.jacid.hrApplication.domain.model.Employee;

public interface GetEmployeesUseCase {
    List<Employee> getAllEmployees();
}
