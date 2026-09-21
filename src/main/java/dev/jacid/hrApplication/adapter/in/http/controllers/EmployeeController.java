package dev.jacid.hrApplication.adapter.in.http.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.adapter.in.http.dto.EmployeeDTO;
import dev.jacid.hrApplication.adapter.in.http.dto.EmployeeRequestDTO;
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
    public List<EmployeeDTO> getEmployees() {
        return employeesUseCases.getAllEmployees().stream().map(mapper::toDto).toList();
    }

    /** The employee record linked to the caller's Keycloak username; 404 if there is none. */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO getCurrentEmployee() {
        return mapper.toDto(employeesUseCases.getCurrentEmployee());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO getEmployee(@PathVariable UUID id) {
        return mapper.toDto(employeesUseCases.getEmployee(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeRequestDTO request) {
        EmployeeDTO created = mapper.toDto(employeesUseCases.createEmployee(mapper.toDomain(request)));
        return ResponseEntity.created(URI.create("/employees/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO updateEmployee(@PathVariable UUID id, @RequestBody EmployeeRequestDTO request) {
        return mapper.toDto(employeesUseCases.updateEmployee(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable UUID id) {
        employeesUseCases.deleteEmployee(id);
    }
}
