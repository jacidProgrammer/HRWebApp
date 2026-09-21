package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.adapter.out.persistence.mappers.EmployeePersistenceMapper;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.model.Employee;

@Repository
@Profile({"h2","postgres","test"})
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeRepositoryJpa employeeRepositoryJpa;
    private final EmployeePersistenceMapper mapper;

    public EmployeeRepositoryAdapter(EmployeeRepositoryJpa employeeRepositoryJpa, EmployeePersistenceMapper mapper) {
        this.employeeRepositoryJpa = employeeRepositoryJpa;
        this.mapper = mapper;
    }

    @Override
    public List<Employee> findAll() {
        return employeeRepositoryJpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Employee> findByName(String name) {
        return employeeRepositoryJpa.findByNameIgnoreCase(name).map(mapper::toDomain);
    }

    @Override
    public Employee save(Employee employee) {
        return mapper.toDomain(employeeRepositoryJpa.save(mapper.toEntity(employee)));
    }

    @Override
    public void deleteByName(String name) {
        employeeRepositoryJpa.deleteByName(name);
    }
}
