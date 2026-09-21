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
import dev.jacid.hrApplication.adapter.in.http.dto.ErrorResponse;
import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackDTO;
import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackRequestDTO;
import dev.jacid.hrApplication.adapter.in.http.mappers.FeedbackDtoMapper;
import dev.jacid.hrApplication.application.port.in.FeedbackUseCases;
import dev.jacid.hrApplication.application.port.in.NewFeedback;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.SentimentFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/feedback")
@Tag(name = "Feedback",
        description = "Peer recognition. The author of anonymous feedback is only ever returned to the author.")
public class FeedbackController {

    private final FeedbackUseCases feedbackUseCases;
    private final FeedbackDtoMapper mapper;

    public FeedbackController(FeedbackUseCases feedbackUseCases, FeedbackDtoMapper mapper) {
        this.feedbackUseCases = feedbackUseCases;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Send feedback to a colleague", description = """
            The caller needs an employee record. The message (1 to 500 characters) is analysed for sentiment when \
            managers enabled it and a Hugging Face token is configured; otherwise, or if the analysis fails, \
            `sentiment` is null.""")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid message or value, missing recipient, or feedback to yourself",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "The caller is not an employee or has no employee record",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Recipient not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackDTO sendFeedback(@RequestBody FeedbackRequestDTO request) {
        NewFeedback feedback = new NewFeedback(request.recipientId(), request.message(),
                FeedbackValue.parse(request.value()), Boolean.TRUE.equals(request.anonymous()));
        return mapper.toDto(feedbackUseCases.sendFeedback(feedback));
    }

    @GetMapping("/received")
    @Operation(summary = "Feedback about the caller", description = "Newest first. Anonymous feedback has no author.")
    @ApiResponse(responseCode = "200", description = "Feedback about the caller")
    @ApiResponse(responseCode = "403", description = "The caller is not an employee or has no employee record",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<FeedbackDTO> getReceivedFeedback() {
        return toDtos(feedbackUseCases.getReceivedFeedback());
    }

    @GetMapping("/sent")
    @Operation(summary = "Feedback written by the caller", description = "Newest first, author always included.")
    @ApiResponse(responseCode = "200", description = "Feedback written by the caller")
    @ApiResponse(responseCode = "403", description = "The caller is not an employee or has no employee record",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<FeedbackDTO> getSentFeedback() {
        return toDtos(feedbackUseCases.getSentFeedback());
    }

    /** All feedback for managers, newest first, optionally filtered. {@code from}/{@code to} are inclusive. */
    @GetMapping
    @Operation(summary = "Search all feedback (managers)",
            description = "Newest first. Anonymous feedback has no author, for managers too.")
    @ApiResponse(responseCode = "200", description = "Matching feedback")
    @ApiResponse(responseCode = "400", description = "Invalid filter value",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('MANAGER')")
    public List<FeedbackDTO> searchFeedback(
            @Parameter(description = "Only feedback about this employee")
            @RequestParam(required = false) UUID recipientId,
            @Parameter(description = "Only feedback about employees of this department (case-insensitive)")
            @RequestParam(required = false) String department,
            @Parameter(description = "Inclusive start: a date (`2026-09-01`, whole UTC day) or a date-time (`2026-09-01T10:15:30Z`)")
            @RequestParam(required = false) String from,
            @Parameter(description = "Inclusive end: a date (whole UTC day) or a date-time")
            @RequestParam(required = false) String to,
            @Parameter(description = "Sentiment label, or `NONE` for feedback that was not analysed",
                    schema = @Schema(allowableValues = {"POSITIVE", "NEUTRAL", "NEGATIVE", "NONE"}))
            @RequestParam(required = false) String sentiment) {
        FeedbackFilter filter = new FeedbackFilter(recipientId, department, QueryParameters.from(from),
                QueryParameters.until(to), SentimentFilter.parse(sentiment));
        return toDtos(feedbackUseCases.searchFeedback(filter));
    }

    private List<FeedbackDTO> toDtos(List<Feedback> feedback) {
        return feedback.stream().map(mapper::toDto).toList();
    }
}
