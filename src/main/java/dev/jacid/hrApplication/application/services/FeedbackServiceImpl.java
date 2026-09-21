package dev.jacid.hrApplication.application.services;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.application.port.in.FeedbackUseCases;
import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.Sentiment;
import jakarta.transaction.Transactional;

@Service
public class FeedbackServiceImpl implements FeedbackUseCases {

    private final EmployeeRepository employeeRepository;
    private final FeedbackRepository feedbackRepository;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final CurrentUserProvider currentUserProvider;

    public FeedbackServiceImpl(EmployeeRepository employeeRepository,
                               FeedbackRepository feedbackRepository,
                               SentimentAnalyzer sentimentAnalyzer,
                               CurrentUserProvider currentUserProvider) {
        this.employeeRepository = employeeRepository;
        this.feedbackRepository = feedbackRepository;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    @Override
    public List<Feedback> getFeedbackByEmployeeName(String name) {
        return feedbackRepository.findByEmployeeName(name);
    }

    /**
     * Stores feedback from the calling employee about {@code employeeName}. The message is
     * enriched with its sentiment when the analyzer is available; otherwise it is stored without it.
     */
    @Override
    @Transactional
    public Feedback sendFeedback(String employeeName, String message) {
        if (employeeName == null || employeeName.isBlank() || message == null || message.isBlank()) {
            throw new InvalidFeedbackException("Feedback needs the employee name and a message");
        }

        Employee employee = employeeRepository.findByName(employeeName)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeName));

        Employee reporter = employeeRepository.findByName(currentUserProvider.currentUser().username())
                .orElseThrow(() -> new OperationNotAllowedException("Only registered employees can send feedback"));

        Sentiment sentiment = sentimentAnalyzer.analyze(message).orElse(null);

        return feedbackRepository.save(new Feedback(null, employee, reporter, message, sentiment));
    }
}
