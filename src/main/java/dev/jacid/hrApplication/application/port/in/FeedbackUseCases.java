package dev.jacid.hrApplication.application.port.in;

import java.util.List;

import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;

public interface FeedbackUseCases {
    /** Stores feedback written by the caller, who must have an employee record. */
    Feedback sendFeedback(NewFeedback feedback);
    /** Feedback about the caller, newest first; anonymous feedback without author. */
    List<Feedback> getReceivedFeedback();
    /** Feedback written by the caller, newest first, always with the author (it is the caller). */
    List<Feedback> getSentFeedback();
    /** Feedback matching the filter (manager view), newest first; anonymous feedback without author. */
    List<Feedback> searchFeedback(FeedbackFilter filter);
}
