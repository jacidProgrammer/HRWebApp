package dev.jacid.hrApplication.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;

import dev.jacid.hrApplication.infrastructure.web.RequestIdFilter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestToEmployeesIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void theApiIsStatelessAndNeverSetsASessionCookie() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(request -> assertThat(request.getRequest().getSession(false)).isNull());
    }

    @Test
    void unauthenticatedRequestToFeedbackIsRejectedWith401() throws Exception {
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jose\",\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointsWithoutExplicitRuleRequireAuthentication() throws Exception {
        // Actuator env used to be reachable anonymously through anyRequest().permitAll()
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void endpointsWithoutExplicitRuleAreAvailableWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isOk());
    }

    @Test
    void h2ConsoleIsNotOpenOutsideTheH2Profile() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void healthProbeIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightFromTheFrontendOriginIsAllowedWithoutToken() throws Exception {
        mockMvc.perform(options("/employees/Jose")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS"));
    }

    @Test
    void corsPreflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/employees")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void unauthenticatedCrossOriginRequestStillCarriesCorsHeaders() throws Exception {
        // Lets the frontend read the 401 status and trigger a new login
        mockMvc.perform(get("/employees").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void rejectedRequestsStillCarryARequestId() throws Exception {
        mockMvc.perform(get("/employees").header(RequestIdFilter.HEADER, "trace-me-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdFilter.HEADER, "trace-me-123"));
    }

    @Test
    void theFrontendMayReadTheRequestIdHeader() throws Exception {
        mockMvc.perform(get("/employees").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString(RequestIdFilter.HEADER)));
    }

    @Test
    void prometheusIsOnlyPublicOnASeparateManagementPort() {
        MockHttpServletRequest onManagementPort = scrape(8081);
        MockHttpServletRequest onApplicationPort = scrape(8080);

        RequestMatcher separatePort = SecurityConfig.prometheusOnManagementPort(
                new MockEnvironment().withProperty("management.server.port", "8081"));
        assertThat(separatePort.matches(onManagementPort)).isTrue();
        assertThat(separatePort.matches(onApplicationPort)).isFalse();
        MockHttpServletRequest otherEndpoint = scrape(8081);
        otherEndpoint.setRequestURI("/actuator/env");
        assertThat(separatePort.matches(otherEndpoint)).isFalse();

        RequestMatcher samePort = SecurityConfig.prometheusOnManagementPort(new MockEnvironment());
        assertThat(samePort.matches(onManagementPort)).isFalse();
        assertThat(samePort.matches(onApplicationPort)).isFalse();
    }

    private static MockHttpServletRequest scrape(int localPort) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setLocalPort(localPort);
        return request;
    }
}
