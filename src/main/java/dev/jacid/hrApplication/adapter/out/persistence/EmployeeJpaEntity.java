package dev.jacid.hrApplication.adapter.out.persistence;

import dev.jacid.hrApplication.domain.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class EmployeeJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String deparment;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String email;

    // Constructores, getters, setters
    public EmployeeJpaEntity() {}

    public EmployeeJpaEntity(Long id, String name, String deparment, String role, String email) {
        this.id = id;
        this.name = name;
        this.deparment = deparment;
        this.role = role;
        this.email = email;
    }

    // Método para convertir a entidad de dominio
    public Employee toDomain() {
        return new Employee(id, name, deparment, role, email);
    }

    // Método para crear desde entidad de dominio
    public static EmployeeJpaEntity fromDomain(Employee employee) {
        return new EmployeeJpaEntity(employee.id(), employee.name(), employee.deparment(), employee.role(), employee.email());
    }
}
