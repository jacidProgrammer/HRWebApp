package dev.jacid.hrApplication.adapter.in.http.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jacid.hrApplication.application.services.HuggingFaceServiceImpl;
import dev.jacid.hrApplication.domain.model.dto.FeedbackDTO;
import dev.jacid.hrApplication.domain.model.dto.HuggingFaceResultDTO;
import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
class FeedbackControllerTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("HRAPI_test")
            .withUsername("admin")
            .withPassword("admin");

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private HuggingFaceServiceImpl huggingFaceServiceImpl;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getFeedbackList() throws Exception {
        mockMvc.perform(get("/feedback"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getFeedbackListFilterByUser() throws Exception {
        mockMvc.perform(get("/feedback/Jose"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name", is("Jose")))
                .andExpect(jsonPath("$[0].message", is("Jose is an excellent Java Backend Developer!")));;
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getFeedbackListFilterByUserAnyMatch() throws Exception {
        mockMvc.perform(get("/feedback/unknown"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void sendNewFeedbackFromNonExistentEmployee() throws Exception {
        // Create a new feedback
        FeedbackDTO feedback = new FeedbackDTO("Maria", "Great work environment and team spirit.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Employee with this name doesn't exists"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void sendNewFeedbackFromNonExistentUser() throws Exception {
        // Create a new feedback
        FeedbackDTO feedback = new FeedbackDTO("Jose", "Great work environment and team spirit.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Reporter with this name doesn't exists"));
    }

    @Test
    @WithMockUser(username = "Louisa", roles = "EMPLOYEE")
    void sendFeedbackSuccesfully() throws Exception {
        HuggingFaceResultDTO sentiment = new HuggingFaceResultDTO("POSITIVE", 0.98);
        given(huggingFaceServiceImpl.analyzeSentiment(anyString()))
                .willReturn(Mono.just(sentiment));
        // Create a new feedback
        FeedbackDTO feedback = new FeedbackDTO("Jose", "Great work environment and team spirit.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Great work environment and team spirit."))
                .andExpect(jsonPath("$.score").value(0.98))
                .andExpect(jsonPath("$.label").value("POSITIVE"));
    }

    @Test
    @WithMockUser(username = "Louisa", roles = "EMPLOYEE")
    void sendFeedbackSuccesfullyWhenNotReachingHuggingFaceAPI() throws Exception {
        HuggingFaceResultDTO sentiment = new HuggingFaceResultDTO(null, null);
        given(huggingFaceServiceImpl.analyzeSentiment(anyString()))
                .willReturn(Mono.just(sentiment));
        // Create a new feedback
        FeedbackDTO feedback = new FeedbackDTO("Jose", "Great work environment and team spirit.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Great work environment and team spirit."))
                .andExpect(jsonPath("$.score").value(nullValue()))
                .andExpect(jsonPath("$.label").value(nullValue()));
    }
}