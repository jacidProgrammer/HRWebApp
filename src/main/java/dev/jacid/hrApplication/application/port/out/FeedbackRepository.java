package dev.jacid.hrApplication.application.port.out;

import java.util.List;

import dev.jacid.hrApplication.adapter.out.persistence.FeedbackJpaEntity;

public interface FeedbackRepository {
    List<FeedbackJpaEntity> findAll();
    List<FeedbackJpaEntity> findByEmployeeName(String name);
    void save(FeedbackJpaEntity feedbackDTO);
}
