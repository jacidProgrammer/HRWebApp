package dev.jacid.hrApplication.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServlet;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void reusesAValidIncomingIdAndExposesItInTheMdcDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/employees");
        request.addHeader(RequestIdFilter.HEADER, "frontend-42.abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                mdcDuringRequest.set(MDC.get(RequestIdFilter.MDC_KEY));
            }
        }));

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("frontend-42.abc");
        assertThat(mdcDuringRequest).hasValue("frontend-42.abc");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).as("cleared after the request").isNull();
    }

    @Test
    void generatesAUuidWhenTheHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/employees"), response, new MockFilterChain());

        assertThat(UUID.fromString(response.getHeader(RequestIdFilter.HEADER))).isNotNull();
    }

    @Test
    void replacesIdsThatCouldInjectIntoTheLogs() {
        assertThat(RequestIdFilter.resolve("abc\nFAKE LOG LINE")).isNotEqualTo("abc\nFAKE LOG LINE");
        assertThat(RequestIdFilter.resolve("x".repeat(65))).hasSize(36);
        assertThat(RequestIdFilter.resolve("")).hasSize(36);
        assertThat(RequestIdFilter.resolve("0af7651916cd43dd8448eb211c80319c")).isEqualTo("0af7651916cd43dd8448eb211c80319c");
    }
}
