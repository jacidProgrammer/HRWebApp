package dev.jacid.hrApplication.adapter.in.http.controllers;

import static dev.jacid.hrApplication.testsupport.Tokens.employee;
import static dev.jacid.hrApplication.testsupport.Tokens.manager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jacid.hrApplication.adapter.in.http.dto.EmployeeRequestDTO;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.domain.model.Employee;

/** Runs against the demo data; every test is rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private FeedbackRepository feedback;

    private Employee jose;
    private Employee louisa;

    @BeforeEach
    void loadDemoEmployees() {
        jose = employees.findByUsername("jose").orElseThrow();
        louisa = employees.findByUsername("louisa").orElseThrow();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    // --- reading ---------------------------------------------------------------------------------

    @Test
    void managersSeeEveryFieldOfEveryEmployee() throws Exception {
        mockMvc.perform(get("/employees").with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[?(@.username == 'louisa')].salary").value(79600.0))
                .andExpect(jsonPath("$[?(@.username == 'louisa')].address").value("Mainz, Germany"));
    }

    @Test
    void employeesSeeSalaryAndAddressOnlyOnTheirOwnRecord() throws Exception {
        mockMvc.perform(get("/employees").with(employee("jose")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'jose')].salary").value(75600.0))
                .andExpect(jsonPath("$[?(@.username == 'louisa')].salary").value(contains(nullValue())))
                .andExpect(jsonPath("$[?(@.username == 'louisa')].address").value(contains(nullValue())));
    }

    @Test
    void employeeJsonHasTheContractFields() throws Exception {
        mockMvc.perform(get("/employees/{id}", jose.id()).with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(jose.id().toString())))
                .andExpect(jsonPath("$.username", is("jose")))
                .andExpect(jsonPath("$.name", is("José Antonio Cid")))
                .andExpect(jsonPath("$.department", is("IT")))
                .andExpect(jsonPath("$.role", is("Java Senior Backend")))
                .andExpect(jsonPath("$.email", is("jose@example.com")))
                .andExpect(jsonPath("$.salary", is(75600.0)))
                .andExpect(jsonPath("$.address", is("Mainz, Germany")))
                .andExpect(jsonPath("$.createdAt", startsWith("20")));
    }

    @Test
    void employeesCanReadOtherEmployeesWithoutSensitiveData() throws Exception {
        mockMvc.perform(get("/employees/{id}", louisa.id()).with(employee("jose")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("louisa")))
                .andExpect(jsonPath("$.salary", nullValue()))
                .andExpect(jsonPath("$.address", nullValue()));
    }

    @Test
    void meReturnsTheRecordLinkedToTheTokenUsernameIgnoringCase() throws Exception {
        mockMvc.perform(get("/employees/me").with(employee("Louisa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(louisa.id().toString())))
                .andExpect(jsonPath("$.salary", is(79600.0)));
    }

    @Test
    void meIs404ForUsersWithoutEmployeeRecord() throws Exception {
        mockMvc.perform(get("/employees/me").with(manager()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("No employee record is linked to user 'manager'")));
    }

    @Test
    void unknownEmployeeIs404() throws Exception {
        UUID unknown = UUID.randomUUID();
        mockMvc.perform(get("/employees/{id}", unknown).with(employee("jose")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Employee '" + unknown + "' not found")));
    }

    @Test
    void malformedIdIs400() throws Exception {
        mockMvc.perform(get("/employees/{id}", "Jose").with(manager()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    // --- creating --------------------------------------------------------------------------------

    @Test
    void managerCreatesAnEmployee() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO("Nora", "Nora Weber", "People", "Recruiter", "nora@example.com",
                52536.89, "Berlin, Germany");

        String location = mockMvc.perform(json(post("/employees"), request).with(manager()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/employees/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("nora")))
                .andExpect(jsonPath("$.name", is("Nora Weber")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location).with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("nora")));
    }

    @Test
    void takenUsernameIs409() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO("LOUISA", "Another Louisa", "IT", "Coach", "l2@example.com", 1.0, "Mainz");

        mockMvc.perform(json(post("/employees"), request).with(manager()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void namesDoNotNeedToBeUnique() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO("louisa2", "Louisa Becker", "IT", "Coach", "l2@example.com", 1.0, "Mainz");

        mockMvc.perform(json(post("/employees"), request).with(manager()))
                .andExpect(status().isCreated());
    }

    @Test
    void missingFieldsAre400() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO("inc", "Incomplete", "IT", null, "i@example.com", null, "Mainz");

        mockMvc.perform(json(post("/employees"), request).with(manager()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Missing required fields: role, salary")));
    }

    @Test
    void employeesCanNotCreateEmployees() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO("nora", "Nora", "People", "Recruiter", "n@example.com", 1.0, "Berlin");

        mockMvc.perform(json(post("/employees"), request).with(employee("jose")))
                .andExpect(status().isForbidden());
    }

    // --- updating --------------------------------------------------------------------------------

    @Test
    void managerUpdatesEverythingButTheUsername() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO(null, "Louisa B.", "People", "Coach", "lb@example.com", 81000.0, "Berlin");

        mockMvc.perform(json(put("/employees/{id}", louisa.id()), request).with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("louisa")))
                .andExpect(jsonPath("$.name", is("Louisa B.")))
                .andExpect(jsonPath("$.department", is("People")))
                .andExpect(jsonPath("$.salary", is(81000.0)))
                .andExpect(jsonPath("$.createdAt", is(louisa.createdAt().toString())));
    }

    @Test
    void managerCanNotChangeTheUsername() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO("lou", "Louisa", "IT", "Coach", "l@example.com", 1.0, "Mainz");

        mockMvc.perform(json(put("/employees/{id}", louisa.id()), request).with(manager()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("username cannot be changed")));
    }

    @Test
    void employeesUpdateTheirOwnEmailAndAddress() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO(null, null, null, null, "jose.new@example.com", null, "Wiesbaden, Germany");

        mockMvc.perform(json(put("/employees/{id}", jose.id()), request).with(employee("jose")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("jose.new@example.com")))
                .andExpect(jsonPath("$.address", is("Wiesbaden, Germany")))
                .andExpect(jsonPath("$.role", is("Java Senior Backend")))
                .andExpect(jsonPath("$.salary", is(75600.0)));
    }

    @Test
    void employeesMaySendTheOtherFieldsUnchanged() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO("jose", jose.name(), jose.department(), jose.role(),
                "jose.new@example.com", jose.salary(), jose.address());

        mockMvc.perform(json(put("/employees/{id}", jose.id()), request).with(employee("jose")))
                .andExpect(status().isOk());
    }

    @Test
    void employeesCanNotChangeAnythingElse() throws Exception {
        EmployeeRequestDTO[] forbidden = {
                new EmployeeRequestDTO(null, "Pepe", null, null, null, null, null),
                new EmployeeRequestDTO(null, null, "Sales", null, null, null, null),
                new EmployeeRequestDTO(null, null, null, "CTO", null, null, null),
                new EmployeeRequestDTO(null, null, null, null, null, 99999.0, null),
                new EmployeeRequestDTO("pepe", null, null, null, null, null, null),
        };
        for (EmployeeRequestDTO request : forbidden) {
            mockMvc.perform(json(put("/employees/{id}", jose.id()), request).with(employee("jose")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", is("Employees can only change their own email and address")));
        }
    }

    @Test
    void employeesCanNotUpdateSomeoneElse() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO(null, null, null, null, "hacked@example.com", null, null);

        mockMvc.perform(json(put("/employees/{id}", louisa.id()), request).with(employee("jose")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only update your own profile")));
    }

    @Test
    void updatingAnUnknownEmployeeIs404() throws Exception {
        EmployeeRequestDTO request = new EmployeeRequestDTO(null, "X", "IT", "Dev", "x@example.com", 1.0, "Mainz");

        mockMvc.perform(json(put("/employees/{id}", UUID.randomUUID()), request).with(manager()))
                .andExpect(status().isNotFound());
    }

    // --- deleting --------------------------------------------------------------------------------

    @Test
    void deletingAnEmployeeRemovesFeedbackAboutThemAndAnonymisesFeedbackByThem() throws Exception {
        int receivedByLouisa = feedback.findByRecipientId(louisa.id()).size();
        long sentByJose = feedback.findByAuthorId(jose.id()).size();
        long total = feedback.count();
        long aboutJose = feedback.findByRecipientId(jose.id()).size();

        mockMvc.perform(delete("/employees/{id}", jose.id()).with(manager()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/employees/{id}", jose.id()).with(manager())).andExpect(status().isNotFound());
        assertThat(feedback.count()).isEqualTo(total - aboutJose);
        assertThat(feedback.findByRecipientId(louisa.id())).hasSize(receivedByLouisa);
        assertThat(sentByJose).isPositive();
        assertThat(feedback.findByAuthorId(jose.id())).isEmpty();
    }

    @Test
    void employeesCanNotDelete() throws Exception {
        mockMvc.perform(delete("/employees/{id}", louisa.id()).with(employee("jose")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingAnUnknownEmployeeIs404() throws Exception {
        mockMvc.perform(delete("/employees/{id}", UUID.randomUUID()).with(manager()))
                .andExpect(status().isNotFound());
    }
}
