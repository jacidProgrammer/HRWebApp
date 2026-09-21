package dev.jacid.hrApplication.adapter.in.http.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackDTO;
import dev.jacid.hrApplication.adapter.in.http.mappers.FeedbackDtoMapper;
import dev.jacid.hrApplication.application.port.in.FeedbackUseCases;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackUseCases feedbackUseCases;
    private final FeedbackDtoMapper mapper;

    public FeedbackController(FeedbackUseCases feedbackUseCases, FeedbackDtoMapper mapper) {
        this.feedbackUseCases = feedbackUseCases;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<FeedbackDTO>> getFeedback() {
        List<FeedbackDTO> feedback = feedbackUseCases.getAllFeedbacks().stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<FeedbackDTO> getFeedbackByEmployeeName(@PathVariable String name) {
        return feedbackUseCases.getFeedbackByEmployeeName(name).stream().map(mapper::toDto).toList();
    }

    @PostMapping("")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public FeedbackDTO sendFeedback(@RequestBody FeedbackDTO feedbackDTO) {
        return mapper.toDto(feedbackUseCases.sendFeedback(feedbackDTO.name(), feedbackDTO.message()));
    }
}
