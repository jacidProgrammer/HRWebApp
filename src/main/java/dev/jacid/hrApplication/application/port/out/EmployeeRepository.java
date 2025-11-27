package dev.jacid.hrApplication.application.port.out;

import java.util.List;

import dev.jacid.hrApplication.adapter.out.persistence.EmployeeJpaEntity;

public interface EmployeeRepository {
    List<EmployeeJpaEntity> findAll();
    EmployeeJpaEntity findByName(String name);
    void save(EmployeeJpaEntity employeeDTO);
    void deleteByName(String name);
}
