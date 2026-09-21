package dev.jacid.hrApplication.application.services;

import static dev.jacid.hrApplication.testsupport.TestData.JOSE;
import static dev.jacid.hrApplication.testsupport.TestData.LOUISA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.domain.exception.InvalidRequestException;
import dev.jacid.hrApplication.domain.model.SentimentLabel;
import dev.jacid.hrApplication.domain.model.stats.StatsOverview;
import dev.jacid.hrApplication.testsupport.TestData;

class StatsServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:15:30Z");

    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final FeedbackRepository feedback = mock(FeedbackRepository.class);
    private final StatsServiceImpl service = new StatsServiceImpl(employees, feedback, () -> NOW);

    @Test
    void loadsOnlyTheFeedbackTheFiguresNeed() {
        given(employees.findAll()).willReturn(List.of(JOSE, LOUISA));
        given(feedback.findCreatedSince(Instant.parse("2026-04-01T00:00:00Z")))
                .willReturn(List.of(TestData.feedback(LOUISA, JOSE, NOW.minusSeconds(60), SentimentLabel.POSITIVE)));
        given(feedback.count()).willReturn(42L);

        StatsOverview overview = service.getOverview(6);

        assertThat(overview.headcount()).isEqualTo(2);
        assertThat(overview.feedback().total()).isEqualTo(42);
        assertThat(overview.feedback().thisMonth()).isEqualTo(1);
        assertThat(overview.trend()).hasSize(6);
    }

    @Test
    void monthsMustBeBetweenOneAndTwelve() {
        for (int months : new int[] {0, -1, 13}) {
            assertThatThrownBy(() -> service.getOverview(months))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage("months must be between 1 and 12");
        }
    }
}
