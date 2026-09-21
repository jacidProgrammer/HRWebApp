package dev.jacid.hrApplication.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.jacid.hrApplication.domain.model.Employee;

public interface EmployeeRepository {
    List<Employee> findAll();
    Optional<Employee> findById(UUID id);
    /** Case-insensitive lookup by the Keycloak username. */
    Optional<Employee> findByUsername(String username);
    long count();
    /** Inserts a new employee (id {@code null}, the id is generated) or updates an existing one. */
    Employee save(Employee employee);
    /**
     * Deletes the employee together with the feedback about them. Feedback they wrote is kept without
     * author, so their colleagues do not lose what they received.
     */
    void deleteById(UUID id);
}
