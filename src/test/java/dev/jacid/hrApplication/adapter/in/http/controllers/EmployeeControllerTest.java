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

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jacid.hrApplication.adapter.in.http.dto.EmployeeDTO;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerTest {

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
                .andExpect(status().isForbidden());
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
    void employeeCanModifyTheirOwnContactDetails() throws Exception {
        // department, role and salary are sent unchanged; only the email changes
        EmployeeDTO employee = new EmployeeDTO("Jose", "IT", "java Senior Backend", "jose.new@test.com", 75600.0, "Mainz, Germany");
        mockMvc.perform(put("/employees/Jose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jose")))
                .andExpect(jsonPath("$.role", is("java Senior Backend")))
                .andExpect(jsonPath("$.salary", is(75600.0)))
                .andExpect(jsonPath("$.email", is("jose.new@test.com")));
    }

    @Test
    @WithMockUser(username = "Jose", roles = "EMPLOYEE")
    void employeeCanNotChangeTheirOwnSalary() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Jose", "IT", "java Senior Backend", "jose@test.com", 99999.0, "Mainz, Germany");
        mockMvc.perform(put("/employees/Jose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                .andExpect(jsonPath("$.message", is("Only managers can change department, role or salary")));
    }

    @Test
    @WithMockUser(username = "Jose", roles = "EMPLOYEE")
    void employeeCanNotChangeTheirOwnRole() throws Exception {
        EmployeeDTO employee = new EmployeeDTO(null, null, "Head of IT", null, null, null);
        mockMvc.perform(put("/employees/Jose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "Jose", roles = "EMPLOYEE")
    void employeeCanNotModifyOtherEmployees() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Louisa", "IT", "Senior Agile Coach", "hacked@test.com", 79600.0, "Mainz, Germany");
        mockMvc.perform(put("/employees/Louisa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only update your own profile")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateWithDifferentNameInBodyIsRejected() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Louisa", "IT", "Backend", "back@test.com", 1.0, "Mainz");
        mockMvc.perform(put("/employees/Jose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updatingUnknownEmployeeReturns404() throws Exception {
        EmployeeDTO employee = new EmployeeDTO(null, "IT", "Backend", "back@test.com", 1.0, "Mainz");
        mockMvc.perform(put("/employees/Nobody")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getUnknownEmployeeReturns404() throws Exception {
        mockMvc.perform(get("/employees/Nobody"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Employee 'Nobody' not found")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void creatingAnExistingEmployeeReturns409() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Louisa", "IT", "Coach", "l@test.com", 1.0, "Mainz");
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void creatingAnEmployeeWithMissingFieldsReturns400() throws Exception {
        EmployeeDTO employee = new EmployeeDTO("Incomplete", "IT", null, "i@test.com", null, "Mainz");
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Missing required fields: role, salary")));
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
        mockMvc.perform(delete("/employees/Jose"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deletingUnknownEmployeeReturns404() throws Exception {
        mockMvc.perform(delete("/employees/Nobody"))
                .andExpect(status().isNotFound());
    }
}