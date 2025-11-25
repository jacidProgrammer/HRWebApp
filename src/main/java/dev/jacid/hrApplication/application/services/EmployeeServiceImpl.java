package dev.jacid.hrApplication.application.services;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.application.port.in.GetEmployeesUseCase;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.domain.model.Employee;

@Service
public class EmployeeServiceImpl implements GetEmployeesUseCase {

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
    
    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }
    
}
