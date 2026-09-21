package dev.jacid.hrApplication.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * The committed OpenAPI contract ({@code docs/openapi.json}) must match what the application serves at
 * {@code /v3/api-docs}. The frontend generates its TypeScript types from the committed file, so a change to the
 * HTTP API that is not reflected there fails the build.
 * <p>
 * Regenerate the file after an intended change with {@code ./mvnw -Pupdate-openapi test}
 * (sets {@code -Dopenapi.update=true}, which writes the file instead of comparing).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    static final Path CONTRACT = Path.of("docs", "openapi.json");

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void committedContractMatchesTheApplication() throws Exception {
        String served = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode actual = JSON.readTree(served);

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, normalized(actual) + "\n", StandardCharsets.UTF_8);
            return;
        }

        assertThat(CONTRACT).as("%s is missing; generate it with ./mvnw -Pupdate-openapi test", CONTRACT).exists();
        JsonNode committed = JSON.readTree(Files.readString(CONTRACT, StandardCharsets.UTF_8));
        assertThat(normalized(actual))
                .as("%s is out of date with the API. If the change is intended, regenerate it with "
                        + "./mvnw -Pupdate-openapi test and commit it (the frontend types are generated from it)", CONTRACT)
                .isEqualTo(normalized(committed));
    }

    /** Pretty-printed with sorted keys, so that the comparison ignores formatting and key order. */
    private static String normalized(JsonNode node) throws Exception {
        return JSON.writeValueAsString(JSON.convertValue(node, Map.class));
    }
}
