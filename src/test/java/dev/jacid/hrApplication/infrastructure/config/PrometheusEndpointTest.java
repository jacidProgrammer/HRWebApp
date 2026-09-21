package dev.jacid.hrApplication.infrastructure.config;

import static dev.jacid.hrApplication.testsupport.Tokens.employee;
import static dev.jacid.hrApplication.testsupport.Tokens.manager;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;

/** {@code /actuator/prometheus} on the application port: token required, business counters exported. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@ActiveProfiles("test")
@Transactional
class PrometheusEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employees;

    @Test
    void scrapingNeedsATokenOnTheApplicationPort() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportsHttpAndFeedbackMetrics() throws Exception {
        String louisa = employees.findByUsername("louisa").orElseThrow().id().toString();
        mockMvc.perform(post("/feedback").with(employee("jose")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":\"" + louisa + "\",\"message\":\"Thanks!\",\"value\":\"TEAMWORK\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/actuator/prometheus").with(manager()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("http_server_requests_seconds_bucket"),
                        containsString("hr_feedback_submitted_total{anonymous=\"false\",application=\"hr-api\",value=\"teamwork\"}"),
                        // no Hugging Face token in the tests
                        containsString("hr_sentiment_analysis_total{application=\"hr-api\",label=\"none\",outcome=\"unavailable\"}"))));
    }
}
