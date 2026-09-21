package dev.jacid.hrApplication.adapter.in.http.dto;

/**
 * Body of {@code POST /employees} (every field required) and {@code PUT /employees/{id}}
 * (the username may be omitted and can never change).
 */
public record EmployeeRequestDTO(String username, String name, String department, String role, String email,
                                 Double salary, String address) {}
