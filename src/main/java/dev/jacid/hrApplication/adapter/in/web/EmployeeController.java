package dev.jacid.hrApplication.adapter.in.web;

import dev.jacid.hrApplication.application.port.in.GetEmployeesUseCase;
import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    
    private final GetEmployeesUseCase getEmployeesUseCase;

    public EmployeeController(GetEmployeesUseCase getEmployeesUseCase) {
        this.getEmployeesUseCase = getEmployeesUseCase;
    }
    
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employees = getEmployeesUseCase.getAllEmployees();
        return ResponseEntity.ok(employees);
    }
}