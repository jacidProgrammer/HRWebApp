package dev.jacid.hrApplication.adapter.in.http.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.adapter.in.http.QueryParameters;
import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackDTO;
import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackRequestDTO;
import dev.jacid.hrApplication.adapter.in.http.mappers.FeedbackDtoMapper;
import dev.jacid.hrApplication.application.port.in.FeedbackUseCases;
import dev.jacid.hrApplication.application.port.in.NewFeedback;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.SentimentFilter;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackUseCases feedbackUseCases;
    private final FeedbackDtoMapper mapper;

    public FeedbackController(FeedbackUseCases feedbackUseCases, FeedbackDtoMapper mapper) {
        this.feedbackUseCases = feedbackUseCases;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackDTO sendFeedback(@RequestBody FeedbackRequestDTO request) {
        NewFeedback feedback = new NewFeedback(request.recipientId(), request.message(),
                FeedbackValue.parse(request.value()), Boolean.TRUE.equals(request.anonymous()));
        return mapper.toDto(feedbackUseCases.sendFeedback(feedback));
    }

    @GetMapping("/received")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<FeedbackDTO> getReceivedFeedback() {
        return toDtos(feedbackUseCases.getReceivedFeedback());
    }

    @GetMapping("/sent")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<FeedbackDTO> getSentFeedback() {
        return toDtos(feedbackUseCases.getSentFeedback());
    }

    /** All feedback for managers, newest first, optionally filtered. {@code from}/{@code to} are inclusive. */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<FeedbackDTO> searchFeedback(@RequestParam(required = false) UUID recipientId,
                                            @RequestParam(required = false) String department,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to,
                                            @RequestParam(required = false) String sentiment) {
        FeedbackFilter filter = new FeedbackFilter(recipientId, department, QueryParameters.from(from),
                QueryParameters.until(to), SentimentFilter.parse(sentiment));
        return toDtos(feedbackUseCases.searchFeedback(filter));
    }

    private List<FeedbackDTO> toDtos(List<Feedback> feedback) {
        return feedback.stream().map(mapper::toDto).toList();
    }
}
