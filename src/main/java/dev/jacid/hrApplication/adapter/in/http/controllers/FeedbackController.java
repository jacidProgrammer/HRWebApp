package dev.jacid.hrApplication.adapter.in.http.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.application.port.in.FeedbackUseCases;
import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;
import dev.jacid.hrApplication.domain.model.dto.FeedbackDTO;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    
    private final FeedbackUseCases feedbackUseCases;

    public FeedbackController(FeedbackUseCases feedbackUseCases) {
        this.feedbackUseCases = feedbackUseCases;
    }
    
    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<FeedbackDTO>> getFeedback() {
        List<FeedbackDTO> feedback = feedbackUseCases.getAllFeedbacks();
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<FeedbackDTO> getEmployeeByName(@PathVariable String name) {
        return feedbackUseCases.getFeedbackByEmployeeName(name);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public FeedbackDTO createEmployee(@RequestBody FeedbackDTO feedbackDTO) {
        return feedbackUseCases.sendFeedback(feedbackDTO);
    }
}