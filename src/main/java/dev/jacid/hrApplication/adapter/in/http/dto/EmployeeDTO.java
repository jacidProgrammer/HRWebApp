package dev.jacid.hrApplication.adapter.in.http.dto;

/** JSON representation of an employee. {@code salary} and {@code address} are {@code null} when hidden. */
public record EmployeeDTO(String name, String department, String role, String email, Double salary, String address) {}
