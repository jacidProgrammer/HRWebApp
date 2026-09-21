package dev.jacid.hrApplication.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.adapter.out.persistence.mappers.FeedbackPersistenceMapper;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;
import dev.jacid.hrApplication.domain.model.SentimentFilter;
import jakarta.persistence.criteria.Predicate;

@Repository
@Profile({"h2","postgres","test"})
public class FeedbackRepositoryAdapter implements FeedbackRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

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
    public List<Feedback> findByRecipientId(UUID recipientId) {
        return toDomain(feedbackRepositoryJpa.findByRecipientIdOrderByCreatedAtDesc(recipientId));
    }

    @Override
    public List<Feedback> findByAuthorId(UUID authorId) {
        return toDomain(feedbackRepositoryJpa.findByAuthorIdOrderByCreatedAtDesc(authorId));
    }

    @Override
    public List<Feedback> search(FeedbackFilter filter) {
        return toDomain(feedbackRepositoryJpa.findAll(matching(filter), NEWEST_FIRST));
    }

    @Override
    public List<Feedback> findCreatedSince(Instant since) {
        return toDomain(feedbackRepositoryJpa.findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(since));
    }

    @Override
    public long count() {
        return feedbackRepositoryJpa.count();
    }

    @Override
    public Feedback save(Feedback feedback) {
        FeedbackJpaEntity entity = mapper.toEntity(feedback);
        entity.setRecipient(employeeRepositoryJpa.getReferenceById(feedback.recipient().id()));
        entity.setAuthor(feedback.author() == null ? null : employeeRepositoryJpa.getReferenceById(feedback.author().id()));
        UUID id = feedbackRepositoryJpa.save(entity).getId();
        return new Feedback(id, feedback.recipient(), feedback.author(), feedback.anonymous(), feedback.value(),
                feedback.message(), feedback.sentiment(), feedback.createdAt());
    }

    private List<Feedback> toDomain(List<FeedbackJpaEntity> entities) {
        return entities.stream().map(mapper::toDomain).toList();
    }

    static Specification<FeedbackJpaEntity> matching(FeedbackFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.recipientId() != null) {
                predicates.add(cb.equal(root.get("recipient").get("id"), filter.recipientId()));
            }
            if (filter.department() != null) {
                predicates.add(cb.equal(cb.lower(root.get("recipient").get("department")),
                        filter.department().toLowerCase(Locale.ROOT)));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
            }
            if (filter.until() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), filter.until()));
            }
            if (filter.sentiment() == SentimentFilter.NONE) {
                predicates.add(cb.isNull(root.get("sentimentLabel")));
            } else if (filter.sentiment() != null) {
                predicates.add(cb.equal(root.get("sentimentLabel"), filter.sentiment().label()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
