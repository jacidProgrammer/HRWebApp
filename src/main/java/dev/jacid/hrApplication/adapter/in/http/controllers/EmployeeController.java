package dev.jacid.hrApplication.adapter.in.http.controllers;

import dev.jacid.hrApplication.application.port.in.EmployeesUseCases;
import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    
    private final EmployeesUseCases employeesUseCases;

    public EmployeeController(EmployeesUseCases employeesUseCases) {
        this.employeesUseCases = employeesUseCases;
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<EmployeeDTO>> getEmployees() {
        List<EmployeeDTO> employees = employeesUseCases.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeDTO getEmployeeByName(@PathVariable String name) {
        return employeesUseCases.getEmployeeByName(name);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeDTO createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        return employeesUseCases.createEmployee(employeeDTO);
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO updateEmployee(@RequestBody EmployeeDTO employeeDTO) {
        return employeesUseCases.updateEmployee(employeeDTO);
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteEmployee(@PathVariable String name) {
        employeesUseCases.deleteEmployeeByName(name);
    }
}