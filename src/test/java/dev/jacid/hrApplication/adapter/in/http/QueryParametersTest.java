package dev.jacid.hrApplication.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.domain.exception.InvalidRequestException;

class QueryParametersTest {

    @Test
    void datesCoverWholeUtcDays() {
        assertThat(QueryParameters.from("2026-09-01")).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(QueryParameters.until("2026-09-01")).isEqualTo(Instant.parse("2026-09-02T00:00:00Z"));
    }

    @Test
    void dateTimesAreInclusiveAndMayHaveAnOffset() {
        assertThat(QueryParameters.from("2026-09-01T10:15:30Z")).isEqualTo(Instant.parse("2026-09-01T10:15:30Z"));
        assertThat(QueryParameters.from("2026-09-01T12:15:30+02:00")).isEqualTo(Instant.parse("2026-09-01T10:15:30Z"));
        assertThat(QueryParameters.until("2026-09-01T10:15:30Z")).isEqualTo(Instant.parse("2026-09-01T10:15:30.000001Z"));
    }

    @Test
    void blankMeansNoBound() {
        assertThat(QueryParameters.from(null)).isNull();
        assertThat(QueryParameters.until(" ")).isNull();
    }

    @Test
    void anythingElseIsRejected() {
        assertThatThrownBy(() -> QueryParameters.from("yesterday")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> QueryParameters.until("2026-13-01")).isInstanceOf(InvalidRequestException.class);
    }
}
