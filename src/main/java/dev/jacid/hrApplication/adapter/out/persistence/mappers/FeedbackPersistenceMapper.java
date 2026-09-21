package dev.jacid.hrApplication.adapter.out.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import dev.jacid.hrApplication.adapter.out.persistence.FeedbackJpaEntity;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.Sentiment;

@Mapper(componentModel = "spring", uses = EmployeePersistenceMapper.class)
public interface FeedbackPersistenceMapper {

    @Mapping(target = "sentiment", source = "entity")
    Feedback toDomain(FeedbackJpaEntity entity);

    /** Employee and reporter are resolved by the adapter as JPA references, so they are not mapped here. */
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "label", source = "sentiment.label")
    @Mapping(target = "score", source = "sentiment.score")
    FeedbackJpaEntity toEntity(Feedback feedback);

    default Sentiment toSentiment(FeedbackJpaEntity entity) {
        if (entity.getLabel() == null && entity.getScore() == null) {
            return null;
        }
        return new Sentiment(entity.getLabel(), entity.getScore());
    }
}
