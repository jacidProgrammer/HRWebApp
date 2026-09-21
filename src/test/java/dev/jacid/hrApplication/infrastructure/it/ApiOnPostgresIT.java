package dev.jacid.hrApplication.infrastructure.it;

import static dev.jacid.hrApplication.testsupport.Tokens.employee;
import static dev.jacid.hrApplication.testsupport.Tokens.manager;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;

/** The whole application with the postgres profile (Flyway, demo data, JPA queries) behind the HTTP API. */
@SpringBootTest(properties = "huggingface.token=")
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
@Transactional
class ApiOnPostgresIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employees;

    @Test
    void dashboardOfTheDemoData() throws Exception {
        mockMvc.perform(get("/stats/overview").with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headcount", is(12)))
                .andExpect(jsonPath("$.feedback.total", is(50)))
                .andExpect(jsonPath("$.trend", hasSize(6)))
                .andExpect(jsonPath("$.alerts[0].name", is("María García")));
    }

    @Test
    void managerFiltersFeedback() throws Exception {
        mockMvc.perform(get("/feedback").param("department", "sales").param("sentiment", "NEGATIVE").with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.anonymous == true)].authorId", everyItem(nullValue())));
    }

    @Test
    void employeeSendsAndReadsFeedback() throws Exception {
        String louisa = employees.findByUsername("louisa").orElseThrow().id().toString();

        mockMvc.perform(post("/feedback").with(employee("jose")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":\"" + louisa + "\",\"message\":\"Thanks for the retro!\",\"value\":\"TEAMWORK\",\"anonymous\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sentiment", nullValue()));

        mockMvc.perform(get("/feedback/received").with(employee("louisa")))
                .andExpect(jsonPath("$[0].message", is("Thanks for the retro!")))
                .andExpect(jsonPath("$[0].authorId", nullValue()));
        mockMvc.perform(get("/employees/me").with(employee("louisa")))
                .andExpect(jsonPath("$.id", is(louisa)));
    }
}
