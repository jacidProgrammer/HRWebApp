package dev.jacid.hrApplication.application.port.out;

import java.util.List;
import java.util.Optional;

import dev.jacid.hrApplication.domain.model.Employee;

public interface EmployeeRepository {
    List<Employee> findAll();
    Optional<Employee> findByName(String name);
    Employee save(Employee employee);
    void deleteByName(String name);
}
