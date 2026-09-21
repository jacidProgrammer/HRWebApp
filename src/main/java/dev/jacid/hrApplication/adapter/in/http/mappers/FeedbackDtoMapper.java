package dev.jacid.hrApplication.adapter.in.http.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackDTO;
import dev.jacid.hrApplication.domain.model.Feedback;

@Mapper(componentModel = "spring")
public interface FeedbackDtoMapper {

    @Mapping(target = "name", source = "employee.name")
    @Mapping(target = "score", source = "sentiment.score")
    @Mapping(target = "label", source = "sentiment.label")
    FeedbackDTO toDto(Feedback feedback);
}
