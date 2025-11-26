package dev.jacid.hrApplication.adapter.out.persistence;

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
    private String department;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String email;
    
    public EmployeeJpaEntity() {}

    public EmployeeJpaEntity(Long id, String name, String department, String role, String email) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.role = role;
        this.email = email;
    }
}
