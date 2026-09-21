package dev.jacid.hrApplication.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Table {@code employees}, created by the Flyway migrations in {@code db/migration}. */
@Entity
@Table(name = "employees")
@Getter
@Setter
public class EmployeeJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Double salary;

    @Column(nullable = false)
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
