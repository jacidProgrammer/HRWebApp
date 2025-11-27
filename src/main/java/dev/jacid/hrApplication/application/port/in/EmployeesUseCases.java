package dev.jacid.hrApplication.application.port.in;

import java.util.List;

import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;

public interface EmployeesUseCases {
    List<EmployeeDTO> getAllEmployees();
    EmployeeDTO getEmployeeByName(String name);
    EmployeeDTO createEmployee(EmployeeDTO beerDTO);
    EmployeeDTO updateEmployee(EmployeeDTO beerDTO);
    void deleteEmployeeByName(String name);
}
