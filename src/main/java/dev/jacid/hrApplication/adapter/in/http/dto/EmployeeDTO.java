package dev.jacid.hrApplication.adapter.in.http.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** JSON representation of an employee. {@code salary} and {@code address} are {@code null} when hidden. */
@Schema(description = "An employee")
public record EmployeeDTO(
        UUID id,
        @Schema(description = "Keycloak username (lower case), never changes", example = "jose") String username,
        @Schema(description = "Display name", example = "José Antonio Cid") String name,
        @Schema(example = "IT") String department,
        @Schema(description = "Job title", example = "Java Senior Backend") String role,
        @Schema(example = "jose@example.com") String email,
        @Schema(description = "Only for managers and the employee themselves, otherwise null", nullable = true, example = "75600.0") Double salary,
        @Schema(description = "Only for managers and the employee themselves, otherwise null", nullable = true, example = "Mainz, Germany") String address,
        Instant createdAt) {}
