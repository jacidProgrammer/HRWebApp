package dev.jacid.hrApplication.adapter.in.http.controllers;

import java.util.List;

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

import dev.jacid.hrApplication.adapter.in.http.dto.EmployeeDTO;
import dev.jacid.hrApplication.adapter.in.http.mappers.EmployeeDtoMapper;
import dev.jacid.hrApplication.application.port.in.EmployeesUseCases;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeesUseCases employeesUseCases;
    private final EmployeeDtoMapper mapper;

    public EmployeeController(EmployeesUseCases employeesUseCases, EmployeeDtoMapper mapper) {
        this.employeesUseCases = employeesUseCases;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<EmployeeDTO>> getEmployees() {
        List<EmployeeDTO> employees = employeesUseCases.getAllEmployees().stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeDTO getEmployeeByName(@PathVariable String name) {
        return mapper.toDto(employeesUseCases.getEmployeeByName(name));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('MANAGER')")
    public EmployeeDTO createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        return mapper.toDto(employeesUseCases.createEmployee(mapper.toDomain(employeeDTO)));
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO updateEmployee(@PathVariable String name, @RequestBody EmployeeDTO employeeDTO) {
        return mapper.toDto(employeesUseCases.updateEmployee(name, mapper.toDomain(employeeDTO)));
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteEmployee(@PathVariable String name) {
        employeesUseCases.deleteEmployeeByName(name);
    }
}
