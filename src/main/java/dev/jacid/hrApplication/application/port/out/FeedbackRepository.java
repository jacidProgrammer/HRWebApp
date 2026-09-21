package dev.jacid.hrApplication.application.port.out;

import java.util.List;

import dev.jacid.hrApplication.domain.model.Feedback;

public interface FeedbackRepository {
    List<Feedback> findAll();
    List<Feedback> findByEmployeeName(String name);
    Feedback save(Feedback feedback);
}
