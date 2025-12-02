package dev.jacid.hrApplication.domain.model.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import dev.jacid.hrApplication.adapter.out.persistence.FeedbackJpaEntity;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    FeedbackMapper INSTANCE = Mappers.getMapper(FeedbackMapper.class);

    @Mapping(target = "name", expression = "java(feedback.getEmployee() != null ? feedback.getEmployee().getName() : null)")
    FeedbackDTO toDto(FeedbackJpaEntity feedback);

    FeedbackJpaEntity toEntity(FeedbackDTO dto);
}
