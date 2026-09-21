package dev.jacid.hrApplication.adapter.in.http.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jacid.hrApplication.adapter.in.http.dto.FeedbackDTO;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.domain.model.Sentiment;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SentimentAnalyzer sentimentAnalyzer;

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
    @WithMockUser(roles = "MANAGER")
    void managersWithoutEmployeeRoleCanNotReadFeedback() throws Exception {
        mockMvc.perform(get("/feedback"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getFeedbackListFilterByUser() throws Exception {
        mockMvc.perform(get("/feedback/Jose"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name", is("Jose")))
                .andExpect(jsonPath("$[0].message", is("Jose is an excellent Java Backend Developer!")))
                .andExpect(jsonPath("$[0].score", is(nullValue())))
                .andExpect(jsonPath("$[0].label", is(nullValue())));
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
    void sendFeedbackAboutNonExistentEmployeeReturns404() throws Exception {
        FeedbackDTO feedback = new FeedbackDTO("Maria", "Great work environment and team spirit.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee 'Maria' not found"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void sendFeedbackFromUserWithoutEmployeeRecordReturns403() throws Exception {
        FeedbackDTO feedback = new FeedbackDTO("Jose", "Great work environment and team spirit.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only registered employees can send feedback"));
    }

    @Test
    @WithMockUser(username = "Louisa", roles = "EMPLOYEE")
    void sendFeedbackWithoutMessageReturns400() throws Exception {
        FeedbackDTO feedback = new FeedbackDTO("Jose", " ");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser(username = "Louisa", roles = "EMPLOYEE")
    void sendFeedbackSuccesfully() throws Exception {
        given(sentimentAnalyzer.analyze(anyString()))
                .willReturn(Optional.of(new Sentiment("POSITIVE", 0.98)));

        FeedbackDTO feedback = new FeedbackDTO("Jose", "Great work environment and team spirit.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jose"))
                .andExpect(jsonPath("$.message").value("Great work environment and team spirit."))
                .andExpect(jsonPath("$.score").value(0.98))
                .andExpect(jsonPath("$.label").value("POSITIVE"));

        then(sentimentAnalyzer).should().analyze("Great work environment and team spirit.");
    }

    @Test
    @WithMockUser(username = "louisa", roles = "EMPLOYEE")
    void sendFeedbackMatchesNamesIgnoringCase() throws Exception {
        given(sentimentAnalyzer.analyze(anyString())).willReturn(Optional.empty());

        FeedbackDTO feedback = new FeedbackDTO("jose", "Always helpful in code reviews.");
        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jose"));
    }

    @Test
    @WithMockUser(username = "Louisa", roles = "EMPLOYEE")
    void sendFeedbackSuccesfullyWhenNotReachingHuggingFaceAPI() throws Exception {
        given(sentimentAnalyzer.analyze(anyString())).willReturn(Optional.empty());

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
