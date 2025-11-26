package dev.jacid.hrApplication.application.port.in;

import java.util.List;

import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;

public interface GetEmployeesUseCase {
    List<EmployeeDTO> getAllEmployees();
}
