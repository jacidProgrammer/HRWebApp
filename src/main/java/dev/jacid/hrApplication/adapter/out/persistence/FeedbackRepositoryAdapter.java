package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.application.port.out.FeedbackRepository;

@Repository
@Profile({"h2","postgres","test"})
public class FeedbackRepositoryAdapter implements FeedbackRepository {

    private final FeedbackRepositoryJpa feedbackRepositoryJpa;

    public FeedbackRepositoryAdapter(FeedbackRepositoryJpa employeeRepositoryJpa) {
        this.feedbackRepositoryJpa = employeeRepositoryJpa;
    }

    @Override
    public List<FeedbackJpaEntity> findAll() {
        return feedbackRepositoryJpa.findAll();
    }

    @Override
    public List<FeedbackJpaEntity> findByEmployeeName(String name) {
        return feedbackRepositoryJpa.findByEmployeeName(name);
    }
    
    @Override
    public void save(FeedbackJpaEntity employeeEntity) {
        feedbackRepositoryJpa.save(employeeEntity);
    }
}
