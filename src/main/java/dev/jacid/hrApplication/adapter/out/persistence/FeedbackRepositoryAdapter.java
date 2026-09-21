package dev.jacid.hrApplication.adapter.out.persistence;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.adapter.out.persistence.mappers.FeedbackPersistenceMapper;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.domain.model.Feedback;

@Repository
@Profile({"h2","postgres","test"})
public class FeedbackRepositoryAdapter implements FeedbackRepository {

    private final FeedbackRepositoryJpa feedbackRepositoryJpa;
    private final EmployeeRepositoryJpa employeeRepositoryJpa;
    private final FeedbackPersistenceMapper mapper;

    public FeedbackRepositoryAdapter(FeedbackRepositoryJpa feedbackRepositoryJpa,
                                     EmployeeRepositoryJpa employeeRepositoryJpa,
                                     FeedbackPersistenceMapper mapper) {
        this.feedbackRepositoryJpa = feedbackRepositoryJpa;
        this.employeeRepositoryJpa = employeeRepositoryJpa;
        this.mapper = mapper;
    }

    @Override
    public List<Feedback> findAll() {
        return feedbackRepositoryJpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Feedback> findByEmployeeName(String name) {
        return feedbackRepositoryJpa.findByEmployeeName(name).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Feedback save(Feedback feedback) {
        FeedbackJpaEntity entity = mapper.toEntity(feedback);
        entity.setEmployee(employeeRepositoryJpa.getReferenceById(feedback.employee().id()));
        entity.setReporter(employeeRepositoryJpa.getReferenceById(feedback.reporter().id()));
        Long id = feedbackRepositoryJpa.save(entity).getId();
        return new Feedback(id, feedback.employee(), feedback.reporter(), feedback.message(), feedback.sentiment());
    }
}
