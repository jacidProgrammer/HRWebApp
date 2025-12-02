package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;

@Repository
@Profile({"h2","postgres","test"})
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeRepositoryJpa employeeRepositoryJpa;

    public EmployeeRepositoryAdapter(EmployeeRepositoryJpa employeeRepositoryJpa) {
        this.employeeRepositoryJpa = employeeRepositoryJpa;
    }

    @Override
    public List<EmployeeJpaEntity> findAll() {
        return employeeRepositoryJpa.findAll();
    }

    @Override
    public EmployeeJpaEntity findByName(String name) {
        return employeeRepositoryJpa.findByNameIgnoreCase(name).orElse(null);
    }

    @Override
    public void save(EmployeeJpaEntity employeeEntity) {
        employeeRepositoryJpa.save(employeeEntity);
    }

    @Override
    public void deleteByName(String name) {
        employeeRepositoryJpa.deleteByName(name);
    }
}
