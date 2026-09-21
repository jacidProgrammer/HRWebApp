package dev.jacid.hrApplication.adapter.in.http.dto;

import java.time.Instant;
import java.util.UUID;

/** JSON representation of an employee. {@code salary} and {@code address} are {@code null} when hidden. */
public record EmployeeDTO(UUID id, String username, String name, String department, String role, String email,
                          Double salary, String address, Instant createdAt) {}
