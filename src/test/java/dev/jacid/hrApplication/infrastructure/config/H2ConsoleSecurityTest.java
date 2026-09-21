package dev.jacid.hrApplication.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * With the h2 profile the console gets its own filter chain: no token required and it may be framed
 * by pages of the same origin. MockMvc does not dispatch to the H2 servlet itself, so the request ends
 * in a 404 after passing the security filters.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:h2ConsoleSecurityTest")
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class H2ConsoleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void h2ConsoleIsReachableWithoutTokenAndFramableFromSameOrigin() throws Exception {
        mockMvc.perform(get("/h2-console/login.do"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void restOfTheApiStillRequiresATokenInTheH2Profile() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
