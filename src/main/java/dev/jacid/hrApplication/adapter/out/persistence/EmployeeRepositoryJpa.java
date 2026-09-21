package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepositoryJpa extends JpaRepository<EmployeeJpaEntity, UUID> {
    Optional<EmployeeJpaEntity> findByUsernameIgnoreCase(String username);
}
