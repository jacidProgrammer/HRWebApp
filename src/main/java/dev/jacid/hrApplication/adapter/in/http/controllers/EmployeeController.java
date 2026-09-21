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
import dev.jacid.hrApplication.adapter.in.http.dto.ErrorResponse;
import dev.jacid.hrApplication.adapter.in.http.mappers.EmployeeDtoMapper;
import dev.jacid.hrApplication.application.port.in.EmployeesUseCases;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/employees")
@Tag(name = "Employees",
        description = "Employee directory. Salary and address are only visible to managers and to the employee themselves.")
public class EmployeeController {

    private final EmployeesUseCases employeesUseCases;
    private final EmployeeDtoMapper mapper;

    public EmployeeController(EmployeesUseCases employeesUseCases, EmployeeDtoMapper mapper) {
        this.employeesUseCases = employeesUseCases;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List employees",
            description = "All employees. `salary` and `address` are null unless the caller is a manager or it is their own record.")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<EmployeeDTO> getEmployees() {
        return employeesUseCases.getAllEmployees().stream().map(mapper::toDto).toList();
    }

    /** The employee record linked to the caller's Keycloak username; 404 if there is none. */
    @GetMapping("/me")
    @Operation(summary = "Get the caller's employee record",
            description = "The employee linked to the token's `preferred_username`.")
    @ApiResponse(responseCode = "200", description = "The caller's employee record")
    @ApiResponse(responseCode = "404", description = "The caller has no employee record (e.g. the manager demo user)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO getCurrentEmployee() {
        return mapper.toDto(employeesUseCases.getCurrentEmployee());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee", description = "Same field visibility as the list.")
    @ApiResponse(responseCode = "200", description = "The employee")
    @ApiResponse(responseCode = "400", description = "The id is not a UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO getEmployee(@Parameter(description = "Employee id") @PathVariable UUID id) {
        return mapper.toDto(employeesUseCases.getEmployee(id));
    }

    @PostMapping
    @Operation(summary = "Create an employee",
            description = "Every field is required. `username` must match a Keycloak username and is stored in lower case.")
    @ApiResponse(responseCode = "201", description = "Created; the Location header points to the new employee")
    @ApiResponse(responseCode = "400", description = "Missing or invalid fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "An employee with this username already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeRequestDTO request) {
        EmployeeDTO created = mapper.toDto(employeesUseCases.createEmployee(mapper.toDomain(request)));
        return ResponseEntity.created(URI.create("/employees/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an employee", description = """
            Managers replace every field except `username` (which can never change). Employees can only update \
            their own `email` and `address`; omitted fields are kept, other fields must be omitted or unchanged.""")
    @ApiResponse(responseCode = "200", description = "The updated employee")
    @ApiResponse(responseCode = "400", description = "Missing or invalid fields, or an attempt to change the username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "The caller's role is not allowed, or an employee updates someone else or a manager-only field",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public EmployeeDTO updateEmployee(@PathVariable UUID id, @RequestBody EmployeeRequestDTO request) {
        return mapper.toDto(employeesUseCases.updateEmployee(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee",
            description = "Deletes the feedback about the employee and keeps the feedback they wrote, without author.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable UUID id) {
        employeesUseCases.deleteEmployee(id);
    }
}
