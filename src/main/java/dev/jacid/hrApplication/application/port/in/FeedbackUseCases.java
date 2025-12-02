package dev.jacid.hrApplication.application.port.in;

import java.util.List;

import dev.jacid.hrApplication.domain.model.dto.FeedbackDTO;

public interface FeedbackUseCases {
    List<FeedbackDTO> getAllFeedbacks();
    List<FeedbackDTO> getFeedbackByEmployeeName(String name);
    FeedbackDTO sendFeedback(FeedbackDTO feedbackDTO);
}
