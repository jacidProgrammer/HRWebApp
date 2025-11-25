package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.model.Employee;

@Repository
@Profile({"h2", "postgres"})
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeRepositoryJpa employeeRepositoryJpa;

    public EmployeeRepositoryAdapter(EmployeeRepositoryJpa employeeRepositoryJpa) {
        this.employeeRepositoryJpa = employeeRepositoryJpa;
    }

    @Override
    public List<Employee> findAll() {
        return employeeRepositoryJpa.findAll()
                .stream()
                .map(EmployeeJpaEntity::toDomain)
                .toList();
    }
}
