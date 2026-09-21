package dev.jacid.hrApplication.adapter.in.http.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackDTO;
import dev.jacid.hrApplication.domain.model.Feedback;

@Mapper(componentModel = "spring")
public interface FeedbackDtoMapper {

    @Mapping(target = "recipientId", source = "recipient.id")
    @Mapping(target = "recipientName", source = "recipient.name")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author.name")
    FeedbackDTO toDto(Feedback feedback);
}
