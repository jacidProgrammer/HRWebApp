package dev.jacid.hrApplication.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.Role;
import dev.jacid.hrApplication.domain.model.Sentiment;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    private static final Employee JOSE =
            new Employee(1L, "Jose", "IT", "Backend", "jose@test.com", 75600.0, "Mainz");
    private static final Employee LOUISA =
            new Employee(2L, "Louisa", "IT", "Agile Coach", "louisa@test.com", 79600.0, "Mainz");
    private static final String MESSAGE = "Great code reviews!";

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private SentimentAnalyzer sentimentAnalyzer;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private FeedbackServiceImpl service;

    @Test
    void getAllFeedbacksDelegatesToRepository() {
        Feedback feedback = new Feedback(1L, JOSE, LOUISA, MESSAGE, null);
        given(feedbackRepository.findAll()).willReturn(List.of(feedback));

        assertThat(service.getAllFeedbacks()).containsExactly(feedback);
    }

    @Test
    void getFeedbackByEmployeeNameDelegatesToRepository() {
        given(feedbackRepository.findByEmployeeName("Nobody")).willReturn(List.of());

        assertThat(service.getFeedbackByEmployeeName("Nobody")).isEmpty();
    }

    @Test
    void sendFeedbackStoresMessageWithSentimentFromAuthenticatedReporter() {
        Sentiment positive = new Sentiment("positive", 0.97);
        given(employeeRepository.findByName("Jose")).willReturn(Optional.of(JOSE));
        given(currentUserProvider.currentUser()).willReturn(new CurrentUser("Louisa", Set.of(Role.EMPLOYEE)));
        given(employeeRepository.findByName("Louisa")).willReturn(Optional.of(LOUISA));
        given(sentimentAnalyzer.analyze(MESSAGE)).willReturn(Optional.of(positive));
        given(feedbackRepository.save(any())).willAnswer(invocation -> {
            Feedback toSave = invocation.getArgument(0);
            return new Feedback(10L, toSave.employee(), toSave.reporter(), toSave.message(), toSave.sentiment());
        });

        Feedback result = service.sendFeedback("Jose", MESSAGE);

        assertThat(result).isEqualTo(new Feedback(10L, JOSE, LOUISA, MESSAGE, positive));
    }

    @Test
    void sendFeedbackIsStoredWithoutSentimentWhenAnalysisIsUnavailable() {
        given(employeeRepository.findByName("Jose")).willReturn(Optional.of(JOSE));
        given(currentUserProvider.currentUser()).willReturn(new CurrentUser("Louisa", Set.of(Role.EMPLOYEE)));
        given(employeeRepository.findByName("Louisa")).willReturn(Optional.of(LOUISA));
        given(sentimentAnalyzer.analyze(MESSAGE)).willReturn(Optional.empty());
        given(feedbackRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.sendFeedback("Jose", MESSAGE);

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        then(feedbackRepository).should().save(saved.capture());
        assertThat(saved.getValue().sentiment()).isNull();
        assertThat(saved.getValue().message()).isEqualTo(MESSAGE);
    }

    @Test
    void sendFeedbackFailsWhenEmployeeDoesNotExist() {
        given(employeeRepository.findByName("Maria")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendFeedback("Maria", MESSAGE))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("Employee 'Maria' not found");
        then(sentimentAnalyzer).should(never()).analyze(anyString());
        then(feedbackRepository).should(never()).save(any());
    }

    @Test
    void sendFeedbackFailsWhenReporterIsNotAnEmployee() {
        given(employeeRepository.findByName("Jose")).willReturn(Optional.of(JOSE));
        given(currentUserProvider.currentUser()).willReturn(new CurrentUser("external-user", Set.of(Role.EMPLOYEE)));
        given(employeeRepository.findByName("external-user")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendFeedback("Jose", MESSAGE))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessage("Only registered employees can send feedback");
        then(feedbackRepository).should(never()).save(any());
    }

    @Test
    void sendFeedbackRejectsBlankMessage() {
        assertThatThrownBy(() -> service.sendFeedback("Jose", "  "))
                .isInstanceOf(InvalidFeedbackException.class);
        then(employeeRepository).shouldHaveNoInteractions();
    }
}
