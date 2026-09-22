package dev.jacid.hrApplication.adapter.in.http.controllers;

import static dev.jacid.hrApplication.testsupport.Tokens.employee;
import static dev.jacid.hrApplication.testsupport.Tokens.manager;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Sentiment;
import dev.jacid.hrApplication.domain.model.SentimentLabel;

/** Runs against the demo data; every test is rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FeedbackControllerTest {

    private static final String MESSAGE = "Great work environment and team spirit.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SentimentAnalyzer sentimentAnalyzer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employees;

    private Employee jose;
    private Employee louisa;
    private Employee maria;

    @BeforeEach
    void loadDemoEmployees() {
        jose = employees.findByUsername("jose").orElseThrow();
        louisa = employees.findByUsername("louisa").orElseThrow();
        maria = employees.findByUsername("maria").orElseThrow();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private ResultActions send(String username, Map<String, Object> body) throws Exception {
        return mockMvc.perform(json(post("/feedback"), body).with(employee(username)));
    }

    private List<Map<String, Object>> list(ResultActions result) throws Exception {
        return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), new TypeReference<>() {});
    }

    // --- sending ---------------------------------------------------------------------------------

    @Test
    void sendFeedbackReturns201WithTheContractFields() throws Exception {
        given(sentimentAnalyzer.analyze(anyString())).willReturn(Optional.of(new Sentiment(SentimentLabel.POSITIVE, 0.98)));

        send("louisa", Map.of("recipientId", jose.id(), "message", MESSAGE, "value", "TEAMWORK"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.recipientId", is(jose.id().toString())))
                .andExpect(jsonPath("$.recipientName", is("José Antonio Cid")))
                .andExpect(jsonPath("$.authorId", is(louisa.id().toString())))
                .andExpect(jsonPath("$.authorName", is("Louisa Becker")))
                .andExpect(jsonPath("$.anonymous", is(false)))
                .andExpect(jsonPath("$.value", is("TEAMWORK")))
                .andExpect(jsonPath("$.message", is(MESSAGE)))
                .andExpect(jsonPath("$.sentiment.label", is("POSITIVE")))
                .andExpect(jsonPath("$.sentiment.score", is(0.98)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        then(sentimentAnalyzer).should().analyze(MESSAGE);
    }

    @Test
    void valueIsOptionalAndSentimentIsNullWhenTheAnalysisIsUnavailable() throws Exception {
        given(sentimentAnalyzer.analyze(anyString())).willReturn(Optional.empty());

        send("louisa", Map.of("recipientId", jose.id(), "message", MESSAGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value", nullValue()))
                .andExpect(jsonPath("$.anonymous", is(false)))
                .andExpect(jsonPath("$.sentiment", nullValue()));
    }

    @Test
    void noSentimentAnalysisWhenAManagerDisabledIt() throws Exception {
        mockMvc.perform(json(put("/settings"), Map.of("sentimentAnalysisEnabled", false)).with(manager()))
                .andExpect(status().isOk());

        send("louisa", Map.of("recipientId", jose.id(), "message", MESSAGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sentiment", nullValue()));

        then(sentimentAnalyzer).should(org.mockito.Mockito.never()).analyze(anyString());
    }

    @Test
    void theAuthorOfAnonymousFeedbackIsReturnedOnlyToTheAuthor() throws Exception {
        given(sentimentAnalyzer.analyze(anyString())).willReturn(Optional.empty());
        String id = objectMapper.readTree(send("louisa", Map.of("recipientId", jose.id(), "message", MESSAGE, "anonymous", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anonymous", is(true)))
                .andExpect(jsonPath("$.authorId", is(louisa.id().toString())))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        // the recipient does not see the author
        mockMvc.perform(get("/feedback/received").with(employee("jose")))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].authorId").value(org.hamcrest.Matchers.contains(nullValue())))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].authorName").value(org.hamcrest.Matchers.contains(nullValue())));
        // neither do managers
        mockMvc.perform(get("/feedback").with(manager()))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].authorId").value(org.hamcrest.Matchers.contains(nullValue())))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].message").value(MESSAGE));
        // the author does
        mockMvc.perform(get("/feedback/sent").with(employee("louisa")))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].authorId").value(louisa.id().toString()))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].authorName").value("Louisa Becker"));
    }

    @Test
    void feedbackToYourselfIs400() throws Exception {
        send("louisa", Map.of("recipientId", louisa.id(), "message", MESSAGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("You cannot send feedback to yourself")));
    }

    @Test
    void invalidMessagesAre400() throws Exception {
        for (String message : new String[] {"", "   ", "x".repeat(501)}) {
            send("louisa", Map.of("recipientId", jose.id(), "message", message))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("message must contain 1 to 500 characters")));
        }
        send("louisa", Map.of("recipientId", jose.id())).andExpect(status().isBadRequest());
    }

    @Test
    void unknownValueIs400() throws Exception {
        send("louisa", Map.of("recipientId", jose.id(), "message", MESSAGE, "value", "KINDNESS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingOrMalformedRecipientIs400() throws Exception {
        send("louisa", Map.of("message", MESSAGE)).andExpect(status().isBadRequest());
        send("louisa", Map.of("recipientId", "Jose", "message", MESSAGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/feedback").with(employee("louisa"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request body")));
    }

    @Test
    void unknownRecipientIs404() throws Exception {
        send("louisa", Map.of("recipientId", UUID.randomUUID(), "message", MESSAGE))
                .andExpect(status().isNotFound());
    }

    @Test
    void employeeWithoutRecordGets403() throws Exception {
        send("someone-else", Map.of("recipientId", jose.id(), "message", MESSAGE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void managersCanNotSendFeedback() throws Exception {
        mockMvc.perform(json(post("/feedback"), Map.of("recipientId", jose.id(), "message", MESSAGE)).with(manager()))
                .andExpect(status().isForbidden());
    }

    // --- received and sent -----------------------------------------------------------------------

    @Test
    void receivedIsTheFeedbackAboutTheCallerNewestFirst() throws Exception {
        List<Map<String, Object>> received = list(mockMvc.perform(get("/feedback/received").with(employee("maria")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(5))))
                .andExpect(jsonPath("$[*].recipientId", everyItem(is(maria.id().toString())))));

        assertNewestFirst(received);
    }

    @Test
    void receivedHidesTheAuthorsOfAnonymousDemoFeedback() throws Exception {
        mockMvc.perform(get("/feedback/received").with(employee("maria")))
                .andExpect(jsonPath("$[?(@.anonymous == true)]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.anonymous == true)].authorId", everyItem(nullValue())))
                .andExpect(jsonPath("$[?(@.anonymous == true)].authorName", everyItem(nullValue())))
                .andExpect(jsonPath("$[?(@.anonymous == false)].authorName", not(hasItem(nullValue()))));
    }

    @Test
    void sentIsTheFeedbackWrittenByTheCallerNewestFirst() throws Exception {
        List<Map<String, Object>> sent = list(mockMvc.perform(get("/feedback/sent").with(employee("jose")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].authorId", everyItem(is(jose.id().toString()))))
                .andExpect(jsonPath("$[?(@.anonymous == true)]", hasSize(1))));

        assertNewestFirst(sent);
    }

    @Test
    void employeesCanNotListEverybodysFeedback() throws Exception {
        mockMvc.perform(get("/feedback").with(employee("jose"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/feedback").param("recipientId", louisa.id().toString()).with(employee("jose")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managersHaveNoReceivedOrSentFeedback() throws Exception {
        mockMvc.perform(get("/feedback/received").with(manager())).andExpect(status().isForbidden());
        mockMvc.perform(get("/feedback/sent").with(manager())).andExpect(status().isForbidden());
    }

    // --- manager list with filters ---------------------------------------------------------------

    @Test
    void managersListAllFeedbackNewestFirst() throws Exception {
        List<Map<String, Object>> all = list(mockMvc.perform(get("/feedback").with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50))));

        assertNewestFirst(all);
    }

    @Test
    void managersFilterByRecipient() throws Exception {
        mockMvc.perform(get("/feedback").param("recipientId", maria.id().toString()).with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[*].recipientName", everyItem(is("María García"))));
    }

    @Test
    void managersFilterByDepartmentIgnoringCase() throws Exception {
        List<Map<String, Object>> it = list(mockMvc.perform(get("/feedback").param("department", "it").with(manager()))
                .andExpect(status().isOk()));
        List<UUID> itIds = employees.findAll().stream().filter(e -> e.department().equals("IT")).map(Employee::id).toList();

        org.assertj.core.api.Assertions.assertThat(it).isNotEmpty()
                .allSatisfy(f -> org.assertj.core.api.Assertions.assertThat(itIds).contains(UUID.fromString((String) f.get("recipientId"))));
    }

    @Test
    void managersFilterBySentiment() throws Exception {
        mockMvc.perform(get("/feedback").param("sentiment", "NEGATIVE").with(manager()))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].sentiment.label", everyItem(is("NEGATIVE"))));
        mockMvc.perform(get("/feedback").param("sentiment", "NONE").with(manager()))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].sentiment", everyItem(nullValue())));
    }

    @Test
    void managersFilterByDateRange() throws Exception {
        List<Map<String, Object>> all = list(mockMvc.perform(get("/feedback").with(manager())));
        Instant from = Instant.parse((String) all.get(20).get("createdAt"));
        Instant to = Instant.parse((String) all.get(10).get("createdAt"));

        mockMvc.perform(get("/feedback").param("from", from.toString()).param("to", to.toString()).with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)));
    }

    @Test
    void invalidFiltersAre400() throws Exception {
        mockMvc.perform(get("/feedback").param("sentiment", "HAPPY").with(manager())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/feedback").param("from", "yesterday").with(manager())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/feedback").param("from", "2026-09-10").param("to", "2026-09-01").with(manager()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/feedback").param("recipientId", "maria").with(manager())).andExpect(status().isBadRequest());
    }

    private static void assertNewestFirst(List<Map<String, Object>> feedback) {
        List<Instant> dates = feedback.stream().map(f -> Instant.parse((String) f.get("createdAt"))).toList();
        org.assertj.core.api.Assertions.assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder());
    }
}
