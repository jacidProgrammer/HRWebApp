package dev.jacid.hrApplication.application.services;

import static dev.jacid.hrApplication.testsupport.TestData.JOSE;
import static dev.jacid.hrApplication.testsupport.TestData.LOUISA;
import static dev.jacid.hrApplication.testsupport.TestData.MARIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.application.port.in.NewFeedback;
import dev.jacid.hrApplication.application.port.out.CurrentUserProvider;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.application.port.out.SettingsRepository;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;
import dev.jacid.hrApplication.domain.model.AppSettings;
import dev.jacid.hrApplication.domain.model.CurrentUser;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.Role;
import dev.jacid.hrApplication.domain.model.Sentiment;
import dev.jacid.hrApplication.domain.model.SentimentLabel;
import dev.jacid.hrApplication.testsupport.TestData;

class FeedbackServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:15:30Z");
    private static final String MESSAGE = "Great code reviews!";
    private static final Sentiment POSITIVE = new Sentiment(SentimentLabel.POSITIVE, 0.97);

    private EmployeeRepository employeeRepository;
    private FeedbackRepository feedbackRepository;
    private SentimentAnalyzer sentimentAnalyzer;
    private SettingsRepository settingsRepository;
    private CurrentUserProvider currentUserProvider;
    private FeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        feedbackRepository = mock(FeedbackRepository.class);
        sentimentAnalyzer = mock(SentimentAnalyzer.class);
        settingsRepository = mock(SettingsRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        service = new FeedbackServiceImpl(employeeRepository, feedbackRepository, sentimentAnalyzer, settingsRepository,
                currentUserProvider, () -> NOW);
        given(feedbackRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(settingsRepository.load()).willReturn(new AppSettings(true));
    }

    private void callerIsLouisa() {
        given(currentUserProvider.currentUser()).willReturn(new CurrentUser("Louisa", Set.of(Role.EMPLOYEE)));
        given(employeeRepository.findByUsername("Louisa")).willReturn(Optional.of(LOUISA));
    }

    // --- sending ---------------------------------------------------------------------------------

    @Test
    void sendFeedbackStoresTheCallerAsAuthorWithSentimentAndCreationDate() {
        callerIsLouisa();
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));
        given(sentimentAnalyzer.analyze(MESSAGE)).willReturn(Optional.of(POSITIVE));

        Feedback result = service.sendFeedback(new NewFeedback(JOSE.id(), "  " + MESSAGE + " ", FeedbackValue.CRAFT, true));

        assertThat(result).isEqualTo(new Feedback(null, JOSE, LOUISA, true, FeedbackValue.CRAFT, MESSAGE, POSITIVE, NOW));
    }

    @Test
    void noSentimentCallWhenManagersDisabledTheAnalysis() {
        callerIsLouisa();
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));
        given(settingsRepository.load()).willReturn(new AppSettings(false));

        Feedback result = service.sendFeedback(new NewFeedback(JOSE.id(), MESSAGE, null, false));

        assertThat(result.sentiment()).isNull();
        then(sentimentAnalyzer).shouldHaveNoInteractions();
    }

    @Test
    void feedbackIsStoredWithoutSentimentWhenTheAnalysisFails() {
        callerIsLouisa();
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));
        given(sentimentAnalyzer.analyze(MESSAGE)).willReturn(Optional.empty());

        assertThat(service.sendFeedback(new NewFeedback(JOSE.id(), MESSAGE, null, false)).sentiment()).isNull();
    }

    @Test
    void sendFeedbackToYourselfIsRejected() {
        callerIsLouisa();
        given(employeeRepository.findById(LOUISA.id())).willReturn(Optional.of(LOUISA));

        assertThatThrownBy(() -> service.sendFeedback(new NewFeedback(LOUISA.id(), MESSAGE, null, true)))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("You cannot send feedback to yourself");
        then(feedbackRepository).should(never()).save(any());
        then(sentimentAnalyzer).shouldHaveNoInteractions();
    }

    @Test
    void sendFeedbackFailsWhenTheRecipientDoesNotExist() {
        callerIsLouisa();
        UUID unknown = UUID.randomUUID();
        given(employeeRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendFeedback(new NewFeedback(unknown, MESSAGE, null, false)))
                .isInstanceOf(EmployeeNotFoundException.class);
        then(sentimentAnalyzer).should(never()).analyze(anyString());
        then(feedbackRepository).should(never()).save(any());
    }

    @Test
    void sendFeedbackFailsWhenTheCallerHasNoEmployeeRecord() {
        given(currentUserProvider.currentUser()).willReturn(new CurrentUser("external", Set.of(Role.EMPLOYEE)));
        given(employeeRepository.findByUsername("external")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendFeedback(new NewFeedback(JOSE.id(), MESSAGE, null, false)))
                .isInstanceOf(OperationNotAllowedException.class);
        then(feedbackRepository).should(never()).save(any());
    }

    @Test
    void messagesMustHaveOneTo500Characters() {
        for (String message : new String[] {null, "", "   ", "x".repeat(501)}) {
            assertThatThrownBy(() -> service.sendFeedback(new NewFeedback(JOSE.id(), message, null, false)))
                    .isInstanceOf(InvalidFeedbackException.class)
                    .hasMessage("message must contain 1 to 500 characters");
        }
        then(employeeRepository).shouldHaveNoInteractions();
    }

    @Test
    void aMessageOfExactly500CharactersIsAccepted() {
        callerIsLouisa();
        given(employeeRepository.findById(JOSE.id())).willReturn(Optional.of(JOSE));
        given(sentimentAnalyzer.analyze(anyString())).willReturn(Optional.empty());

        assertThat(service.sendFeedback(new NewFeedback(JOSE.id(), "x".repeat(500), null, false)).message()).hasSize(500);
    }

    @Test
    void theRecipientIsRequired() {
        assertThatThrownBy(() -> service.sendFeedback(new NewFeedback(null, MESSAGE, null, false)))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("recipientId is required");
    }

    // --- reading ---------------------------------------------------------------------------------

    @Test
    void receivedFeedbackHidesTheAuthorOfAnonymousFeedback() {
        callerIsLouisa();
        Feedback anonymous = TestData.feedback(LOUISA, JOSE, NOW, SentimentLabel.POSITIVE, null, true);
        Feedback named = TestData.feedback(LOUISA, MARIA, NOW.minusSeconds(60), SentimentLabel.POSITIVE, null, false);
        given(feedbackRepository.findByRecipientId(LOUISA.id())).willReturn(List.of(anonymous, named));

        List<Feedback> received = service.getReceivedFeedback();

        assertThat(received).extracting(Feedback::author).containsExactly(null, MARIA);
    }

    @Test
    void sentFeedbackKeepsTheAuthorEvenWhenAnonymous() {
        callerIsLouisa();
        Feedback anonymous = TestData.feedback(JOSE, LOUISA, NOW, SentimentLabel.POSITIVE, null, true);
        given(feedbackRepository.findByAuthorId(LOUISA.id())).willReturn(List.of(anonymous));

        assertThat(service.getSentFeedback()).containsExactly(anonymous);
    }

    @Test
    void employeesOnlyReadTheirOwnFeedback() {
        callerIsLouisa();
        given(feedbackRepository.findByRecipientId(LOUISA.id())).willReturn(List.of());
        given(feedbackRepository.findByAuthorId(LOUISA.id())).willReturn(List.of());

        service.getReceivedFeedback();
        service.getSentFeedback();

        then(feedbackRepository).should().findByRecipientId(LOUISA.id());
        then(feedbackRepository).should().findByAuthorId(LOUISA.id());
        then(feedbackRepository).should(never()).search(any());
    }

    @Test
    void usersWithoutEmployeeRecordCanNotReadFeedback() {
        given(currentUserProvider.currentUser()).willReturn(new CurrentUser("manager", Set.of(Role.MANAGER)));
        given(employeeRepository.findByUsername("manager")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReceivedFeedback()).isInstanceOf(OperationNotAllowedException.class);
        assertThatThrownBy(() -> service.getSentFeedback()).isInstanceOf(OperationNotAllowedException.class);
    }

    @Test
    void managerSearchNeverRevealsTheAuthorOfAnonymousFeedback() {
        Feedback anonymous = TestData.feedback(LOUISA, JOSE, NOW, SentimentLabel.NEGATIVE, null, true);
        Feedback named = TestData.feedback(MARIA, JOSE, NOW, SentimentLabel.POSITIVE, null, false);
        FeedbackFilter filter = new FeedbackFilter(null, "IT", null, null, null);
        given(feedbackRepository.search(filter)).willReturn(List.of(anonymous, named));

        List<Feedback> result = service.searchFeedback(filter);

        assertThat(result).extracting(Feedback::author).containsExactly(null, JOSE);
        assertThat(result.get(0).message()).isEqualTo(anonymous.message());
    }

    @Test
    void searchWithoutFilterMatchesEverything() {
        given(feedbackRepository.search(FeedbackFilter.none())).willReturn(List.of());

        assertThat(service.searchFeedback(null)).isEmpty();
    }
}
