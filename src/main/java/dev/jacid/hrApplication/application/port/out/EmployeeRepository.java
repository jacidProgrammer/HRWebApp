package dev.jacid.hrApplication.application.port.out;

import java.util.List;

import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;

public interface EmployeeRepository {
    List<EmployeeDTO> findAll();
}
