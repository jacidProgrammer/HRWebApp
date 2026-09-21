package dev.jacid.hrApplication.adapter.in.http.controllers;

import static dev.jacid.hrApplication.testsupport.Tokens.employee;
import static dev.jacid.hrApplication.testsupport.Tokens.manager;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** The aggregation rules are unit tested in StatsCalculatorTest; this checks the endpoint on the demo data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void overviewOfTheDemoData() throws Exception {
        mockMvc.perform(get("/stats/overview").with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headcount", is(12)))
                .andExpect(jsonPath("$.departments", hasSize(4)))
                .andExpect(jsonPath("$.departments[0].name", is("Finance")))
                .andExpect(jsonPath("$.departments[0].headcount", is(3)))
                .andExpect(jsonPath("$.feedback.total", is(50)))
                .andExpect(jsonPath("$.sentimentShare.positive").isNumber())
                .andExpect(jsonPath("$.sentimentShare.notAnalysed").isNumber())
                .andExpect(jsonPath("$.trend", hasSize(6)))
                .andExpect(jsonPath("$.trend[*].month", everyItem(matchesPattern("\\d{4}-\\d{2}"))))
                .andExpect(jsonPath("$.valueCounts", hasSize(5)))
                .andExpect(jsonPath("$.topRecognised", hasSize(5)))
                .andExpect(jsonPath("$.topRecognised[0].name", is("María García")))
                .andExpect(jsonPath("$.topRecognised[1].name", is("Louisa Becker")))
                .andExpect(jsonPath("$.topRecognised[1].count", is(9)))
                .andExpect(jsonPath("$.topRecognised[1].positiveShare", is(0.89)))
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].name", is("María García")))
                .andExpect(jsonPath("$.alerts[0].department", is("Sales")))
                .andExpect(jsonPath("$.alerts[0].previousPositiveShare", is(0.8)))
                .andExpect(jsonPath("$.alerts[0].currentPositiveShare", is(0.4)))
                .andExpect(jsonPath("$.alerts[0].feedbackCount", is(5)))
                .andExpect(jsonPath("$.alerts[0].message").doesNotExist());
    }

    @Test
    void monthsSelectsTheLengthOfTheTrend() throws Exception {
        mockMvc.perform(get("/stats/overview").param("months", "12").with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend", hasSize(12)))
                .andExpect(jsonPath("$.trend[*].positive", everyItem(lessThanOrEqualTo(50))));
        mockMvc.perform(get("/stats/overview").param("months", "1").with(manager()))
                .andExpect(jsonPath("$.trend", hasSize(1)));
    }

    @Test
    void monthsOutOfRangeAre400() throws Exception {
        for (String months : new String[] {"0", "13", "six"}) {
            mockMvc.perform(get("/stats/overview").param("months", months).with(manager()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
        }
    }

    @Test
    void onlyManagersSeeTheDashboard() throws Exception {
        mockMvc.perform(get("/stats/overview").with(employee("jose"))).andExpect(status().isForbidden());
    }
}
