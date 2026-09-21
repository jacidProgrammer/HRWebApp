package dev.jacid.hrApplication.application.port.in;

import java.util.List;

import dev.jacid.hrApplication.domain.model.Feedback;

public interface FeedbackUseCases {
    List<Feedback> getAllFeedbacks();
    List<Feedback> getFeedbackByEmployeeName(String name);
    Feedback sendFeedback(String employeeName, String message);
}
