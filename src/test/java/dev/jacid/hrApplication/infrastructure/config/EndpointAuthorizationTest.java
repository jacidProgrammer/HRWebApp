package dev.jacid.hrApplication.infrastructure.config;

import static dev.jacid.hrApplication.testsupport.Tokens.employee;
import static dev.jacid.hrApplication.testsupport.Tokens.manager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;

/**
 * The role matrix of API contract v2: every endpoint without token (401), as MANAGER and as EMPLOYEE
 * (allowed = anything but 401/403, forbidden = 403). Requests are rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndpointAuthorizationTest {

    private static final boolean ALLOWED = true;
    private static final boolean FORBIDDEN = false;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employees;

    private String louisaId;

    @BeforeEach
    void setUp() {
        louisaId = employees.findByUsername("louisa").orElseThrow().id().toString();
    }

    static Stream<Arguments> endpoints() {
        String employeeBody = "{\"username\":\"new.one\",\"name\":\"New One\",\"department\":\"IT\",\"role\":\"Dev\","
                + "\"email\":\"new@example.com\",\"salary\":1,\"address\":\"Mainz\"}";
        String feedbackBody = "{\"recipientId\":\"{louisa}\",\"message\":\"Thanks!\"}";
        return Stream.of(
                //            method             path                   body                                    MANAGER    EMPLOYEE
                Arguments.of(HttpMethod.GET, "/employees", null, ALLOWED, ALLOWED),
                Arguments.of(HttpMethod.GET, "/employees/me", null, ALLOWED, ALLOWED),
                Arguments.of(HttpMethod.GET, "/employees/{louisa}", null, ALLOWED, ALLOWED),
                Arguments.of(HttpMethod.POST, "/employees", employeeBody, ALLOWED, FORBIDDEN),
                Arguments.of(HttpMethod.PUT, "/employees/{louisa}", "{\"email\":\"l@example.com\"}", ALLOWED, ALLOWED),
                Arguments.of(HttpMethod.DELETE, "/employees/{louisa}", null, ALLOWED, FORBIDDEN),
                Arguments.of(HttpMethod.POST, "/feedback", feedbackBody, FORBIDDEN, ALLOWED),
                Arguments.of(HttpMethod.GET, "/feedback/received", null, FORBIDDEN, ALLOWED),
                Arguments.of(HttpMethod.GET, "/feedback/sent", null, FORBIDDEN, ALLOWED),
                Arguments.of(HttpMethod.GET, "/feedback", null, ALLOWED, FORBIDDEN),
                Arguments.of(HttpMethod.GET, "/stats/overview", null, ALLOWED, FORBIDDEN),
                Arguments.of(HttpMethod.GET, "/settings", null, ALLOWED, ALLOWED),
                Arguments.of(HttpMethod.PUT, "/settings", "{\"sentimentAnalysisEnabled\":true}", ALLOWED, FORBIDDEN));
    }

    private MockHttpServletRequestBuilder call(HttpMethod method, String path, String body) {
        MockHttpServletRequestBuilder builder = request(method, path.replace("{louisa}", louisaId));
        return body == null ? builder
                : builder.contentType(MediaType.APPLICATION_JSON).content(body.replace("{louisa}", louisaId));
    }

    private int status(HttpMethod method, String path, String body, RequestPostProcessor token) throws Exception {
        MockHttpServletRequestBuilder builder = call(method, path, body);
        if (token != null) {
            builder.with(token);
        }
        return mockMvc.perform(builder).andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void withoutTokenIs401(HttpMethod method, String path, String body, boolean managerAllowed, boolean employeeAllowed)
            throws Exception {
        assertThat(status(method, path, body, null)).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void asManager(HttpMethod method, String path, String body, boolean managerAllowed, boolean employeeAllowed)
            throws Exception {
        int status = status(method, path, body, manager());
        if (managerAllowed) {
            assertThat(status).isNotIn(401, 403);
        } else {
            assertThat(status).isEqualTo(403);
        }
    }

    /** As jose, an employee with a record; PUT on louisa is allowed by role but refused by the ownership rule. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void asEmployee(HttpMethod method, String path, String body, boolean managerAllowed, boolean employeeAllowed)
            throws Exception {
        int status = status(method, path, body, employee("jose"));
        if (method == HttpMethod.PUT && path.startsWith("/employees/")) {
            assertThat(status).isEqualTo(403); // someone else's record
            assertThat(status(method, path, body, employee("louisa"))).isEqualTo(200); // own record
        } else if (employeeAllowed) {
            assertThat(status).isNotIn(401, 403);
        } else {
            assertThat(status).isEqualTo(403);
        }
    }
}
