package dev.jacid.hrApplication.adapter.in.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of {@code POST /employees} (every field required) and {@code PUT /employees/{id}}
 * (the username may be omitted and can never change).
 */
@Schema(description = "Employee to create (every field required) or update (see the operation for the rules)")
public record EmployeeRequestDTO(
        @Schema(description = "Keycloak username: letters, digits, `. _ @ -`. Required on create, immutable afterwards", example = "jose") String username,
        @Schema(example = "José Antonio Cid") String name,
        @Schema(example = "IT") String department,
        @Schema(description = "Job title", example = "Java Senior Backend") String role,
        @Schema(example = "jose@example.com") String email,
        @Schema(example = "75600.0") Double salary,
        @Schema(example = "Mainz, Germany") String address) {}
