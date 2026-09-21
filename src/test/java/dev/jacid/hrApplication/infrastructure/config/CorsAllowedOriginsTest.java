package dev.jacid.hrApplication.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The allowed origins come from {@code app.cors.allowed-origins}, a comma-separated list. */
@SpringBootTest(properties = "app.cors.allowed-origins=https://hr.example.com, http://localhost:4173")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsAllowedOriginsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyConfiguredOriginIsAllowed() throws Exception {
        for (String origin : new String[] {"https://hr.example.com", "http://localhost:4173"}) {
            mockMvc.perform(options("/feedback")
                            .header(HttpHeaders.ORIGIN, origin)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        }
    }

    @Test
    void theDefaultDevOriginIsNoLongerAllowedWhenOverridden() throws Exception {
        mockMvc.perform(options("/feedback")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
