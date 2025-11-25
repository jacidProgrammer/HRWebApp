package dev.jacid.hrApplication.application.port.out;

import java.util.List;

import dev.jacid.hrApplication.domain.model.Employee;

public interface EmployeeRepository {
    List<Employee> findAll();
}
