package dev.jacid.hrApplication.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepositoryJpa extends JpaRepository<EmployeeJpaEntity, Long> {
}
