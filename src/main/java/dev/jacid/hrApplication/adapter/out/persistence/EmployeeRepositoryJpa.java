package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepositoryJpa extends JpaRepository<EmployeeJpaEntity, Long> {
    Optional<EmployeeJpaEntity> findByName(String name);
    void deleteByName(String name);
}
