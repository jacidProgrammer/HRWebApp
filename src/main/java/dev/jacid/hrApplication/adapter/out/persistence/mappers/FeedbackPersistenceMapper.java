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

    /** Recipient and author are resolved by the adapter as JPA references, so they are not mapped here. */
    @Mapping(target = "recipient", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "sentimentLabel", source = "sentiment.label")
    @Mapping(target = "sentimentScore", source = "sentiment.score")
    FeedbackJpaEntity toEntity(Feedback feedback);

    default Sentiment toSentiment(FeedbackJpaEntity entity) {
        if (entity.getSentimentLabel() == null) {
            return null;
        }
        return new Sentiment(entity.getSentimentLabel(), entity.getSentimentScore() == null ? 0 : entity.getSentimentScore());
    }
}
