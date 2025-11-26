package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;

import dev.jacid.hrApplication.domain.model.dto.EmployeeMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;

@Repository
@Profile({"h2","postgres"})
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final EmployeeRepositoryJpa employeeRepositoryJpa;

    public EmployeeRepositoryAdapter(EmployeeRepositoryJpa employeeRepositoryJpa) {
        this.employeeRepositoryJpa = employeeRepositoryJpa;
    }

    @Override
    public List<EmployeeDTO> findAll() {
        return employeeRepositoryJpa.findAll()
                .stream()
                .map(EmployeeMapper.INSTANCE::toDto)
                .toList();
    }
}
