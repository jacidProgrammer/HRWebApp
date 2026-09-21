package dev.jacid.hrApplication.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.application.port.in.FeedbackUseCases;
import dev.jacid.hrApplication.application.port.in.NewFeedback;
import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.application.port.out.SettingsRepository;
import dev.jacid.hrApplication.application.port.out.TimeProvider;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;
import dev.jacid.hrApplication.domain.model.Sentiment;

/**
 * Feedback use cases. Visibility rules:
 * <ul>
 *   <li>employees only read feedback about themselves or written by themselves;</li>
 *   <li>the author of anonymous feedback is only returned to the author (in their sent feedback);</li>
 *   <li>managers read all feedback, never with the author of anonymous feedback.</li>
 * </ul>
 */
@Service
public class FeedbackServiceImpl implements FeedbackUseCases {

    static final String NO_EMPLOYEE_RECORD = "Only users with an employee record can send or read feedback";

    private final EmployeeRepository employeeRepository;
    private final FeedbackRepository feedbackRepository;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final SettingsRepository settingsRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TimeProvider timeProvider;

    public FeedbackServiceImpl(EmployeeRepository employeeRepository,
                               FeedbackRepository feedbackRepository,
                               SentimentAnalyzer sentimentAnalyzer,
                               SettingsRepository settingsRepository,
                               CurrentUserProvider currentUserProvider,
                               TimeProvider timeProvider) {
        this.employeeRepository = employeeRepository;
        this.feedbackRepository = feedbackRepository;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.settingsRepository = settingsRepository;
        this.currentUserProvider = currentUserProvider;
        this.timeProvider = timeProvider;
    }

    /**
     * Stores feedback from the calling employee. The message is analysed only when managers enabled
     * sentiment analysis; if they did not, or the analysis fails, it is stored without sentiment.
     * Not transactional on purpose: the (slow) external call must not hold a database transaction open,
     * and the single insert is atomic anyway.
     */
    @Override
    public Feedback sendFeedback(NewFeedback request) {
        String message = request.message() == null ? "" : request.message().strip();
        if (request.recipientId() == null) {
            throw new InvalidFeedbackException("recipientId is required");
        }
        if (message.isEmpty() || message.length() > Feedback.MAX_MESSAGE_LENGTH) {
            throw new InvalidFeedbackException("message must contain 1 to " + Feedback.MAX_MESSAGE_LENGTH + " characters");
        }
        Employee author = callerRecord();
        Employee recipient = employeeRepository.findById(request.recipientId())
                .orElseThrow(() -> EmployeeNotFoundException.withId(request.recipientId()));
        if (recipient.id().equals(author.id())) {
            throw new InvalidFeedbackException("You cannot send feedback to yourself");
        }

        Sentiment sentiment = settingsRepository.load().sentimentAnalysisEnabled()
                ? sentimentAnalyzer.analyze(message).orElse(null)
                : null;

        return feedbackRepository.save(new Feedback(null, recipient, author, request.anonymous(), request.value(),
                message, sentiment, timeProvider.now()));
    }

    @Override
    public List<Feedback> getReceivedFeedback() {
        return feedbackRepository.findByRecipientId(callerRecord().id()).stream().map(Feedback::withAuthorHidden).toList();
    }

    @Override
    public List<Feedback> getSentFeedback() {
        return feedbackRepository.findByAuthorId(callerRecord().id());
    }

    @Override
    public List<Feedback> searchFeedback(FeedbackFilter filter) {
        return feedbackRepository.search(filter == null ? FeedbackFilter.none() : filter).stream()
                .map(Feedback::withAuthorHidden)
                .toList();
    }

    private Employee callerRecord() {
        String username = currentUserProvider.currentUser().username();
        return (username == null ? Optional.<Employee>empty() : employeeRepository.findByUsername(username))
                .orElseThrow(() -> new OperationNotAllowedException(NO_EMPLOYEE_RECORD));
    }
}
