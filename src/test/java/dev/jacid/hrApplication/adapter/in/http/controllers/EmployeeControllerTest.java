package dev.jacid.hrApplication.adapter.in.http.controllers;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jacid.hrApplication.domain.model.dto.EmployeeDTO;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class EmployeeControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("HRAPI_test")
            .withUsername("admin")
            .withPassword("admin");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(roles = "MANAGER")
    void getEmployeesReturnsEmployeeListWithAllDataForManagers() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].salary", is(75600.0)))
                .andExpect(jsonPath("$[0].address", is("Mainz, Germany")));
    }

    @Test
    @WithMockUser(username = "Jose", roles = "EMPLOYEE")
    void getEmployeesReturnsEmployeeListWithSensibleDataForCurrentUser() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].salary", is(75600.0)))
                .andExpect(jsonPath("$[0].address", is("Mainz, Germany")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getEmployeesReturnsEmployeeListWithoutSensibleData() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].salary", is(nullValue())))
                .andExpect(jsonPath("$[0].address", is(nullValue())));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createEmployeeandGetEmployeByName() throws Exception {
        // Create a new employee
        EmployeeDTO employee = new EmployeeDTO("Maria", "HR", "Recruiter", "recruiter@test.com", 52536.89, "Berlin");
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Maria")));

        // get employee by name
        mockMvc.perform(get("/employees/Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Maria")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCanNotCreate() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Maria", "HR", "Recruiter", "recruiter@test.com", 52536.89, "Berlin");
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanUpdateEmployeeData() throws Exception {
        // Create a new employee
        EmployeeDTO employee = new EmployeeDTO("TestEmployeeUpdate", "IT", "Frontend", "front@test.com", 63444.50, "Munich");
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("TestEmployeeUpdate")));

        employee = new EmployeeDTO("TestEmployeeUpdate",  "IT", "Backend", "back@test.com", 63444.50, "Munich");
        mockMvc.perform(put("/employees/TestEmployeeUpdate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("TestEmployeeUpdate")))
                .andExpect(jsonPath("$.department", is("IT")))
                .andExpect(jsonPath("$.role", is("Backend")))
                .andExpect(jsonPath("$.email", is("back@test.com")));
    }

    @Test
    @WithMockUser(username = "Jose", roles = "EMPLOYEE")
    void employeeCanModifyTheirOwnData() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Jose", "IT", "Senior Java", "back@test.com", 75600.0, "Mainz, Germany");
        mockMvc.perform(put("/employees/Jose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jose")))
                .andExpect(jsonPath("$.department", is("IT")))
                .andExpect(jsonPath("$.role", is("Senior Java")))
                .andExpect(jsonPath("$.email", is("back@test.com")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCanNotModifyOtherEmployees() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Jose", "HR", "Recruiter", "recruiter@test.com", 52536.89, "Berlin");
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanDelete() throws Exception {
        // Create a new employee
        EmployeeDTO employee = new EmployeeDTO("TestEmployeeDelete", "Sales", "Call Center", "sales@test.com", 47890.00, "Hamburg");
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("TestEmployeeDelete")));

        // Delete the employee
        mockMvc.perform(delete("/employees/TestEmployeeDelete"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCanNotdelete() throws Exception {
        // Create a new employee
        EmployeeDTO employee = new EmployeeDTO("TestEmployeeDelete", "Sales", "Call Center", "sales@test.com", 47890.00, "Hamburg");
        // Delete the employee
        mockMvc.perform(delete("/employees/Jose"))
                .andExpect(status().is4xxClientError());
    }
}