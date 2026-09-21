package dev.jacid.hrApplication.adapter.in.http.controllers;

import static dev.jacid.hrApplication.testsupport.Tokens.employee;
import static dev.jacid.hrApplication.testsupport.Tokens.manager;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Every test is rolled back, so the stored setting is back to "enabled" afterwards. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyRoleReadsTheSettings() throws Exception {
        // the test profile has no Hugging Face token
        for (RequestPostProcessor token : List.of(manager(), employee("jose"))) {
            mockMvc.perform(get("/settings").with(token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sentimentAnalysisEnabled", is(true)))
                    .andExpect(jsonPath("$.sentimentAnalysisAvailable", is(false)));
        }
    }

    @Test
    void managersDisableTheAnalysisAndTheChangeIsPersisted() throws Exception {
        mockMvc.perform(put("/settings").with(manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentimentAnalysisEnabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentimentAnalysisEnabled", is(false)))
                .andExpect(jsonPath("$.sentimentAnalysisAvailable", is(false)));

        mockMvc.perform(get("/settings").with(employee("jose")))
                .andExpect(jsonPath("$.sentimentAnalysisEnabled", is(false)));
    }

    @Test
    void theFlagIsRequired() throws Exception {
        mockMvc.perform(put("/settings").with(manager()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("sentimentAnalysisEnabled is required")));
    }

    @Test
    void employeesCanNotChangeTheSettings() throws Exception {
        mockMvc.perform(put("/settings").with(employee("jose"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentimentAnalysisEnabled\": false}"))
                .andExpect(status().isForbidden());
    }
}
