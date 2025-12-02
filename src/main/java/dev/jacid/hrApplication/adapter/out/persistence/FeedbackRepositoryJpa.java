package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepositoryJpa extends JpaRepository<FeedbackJpaEntity, Long> {
    List<FeedbackJpaEntity> findByEmployeeName(String name);
}
