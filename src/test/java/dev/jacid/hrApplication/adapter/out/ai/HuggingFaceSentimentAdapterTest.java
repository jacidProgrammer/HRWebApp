package dev.jacid.hrApplication.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import dev.jacid.hrApplication.domain.model.Sentiment;
import dev.jacid.hrApplication.domain.model.SentimentLabel;

class HuggingFaceSentimentAdapterTest {

    private static final String BASE_URL = "https://hf.test";
    private static final String MODEL = "org/sentiment-model";
    private static final String ENDPOINT = BASE_URL + "/hf-inference/models/" + MODEL;

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private HuggingFaceSentimentAdapter adapterWithToken(String token) {
        return new HuggingFaceSentimentAdapter(restClient,
                new HuggingFaceProperties(BASE_URL, token, MODEL, Duration.ofSeconds(5)));
    }

    @Test
    void returnsTheTopScoredLabel() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"inputs\":\"Great job\",\"wait_for_model\":true}"))
                .andRespond(withSuccess("""
                        [[{"label":"positive","score":0.97},{"label":"neutral","score":0.02}]]
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapterWithToken("secret").analyze("Great job"))
                .contains(new Sentiment(SentimentLabel.POSITIVE, 0.97));
        server.verify();
    }

    @Test
    void returnsEmptyWhenTheApiAnswersWithAnError() {
        server.expect(requestTo(ENDPOINT)).andRespond(withUnauthorizedRequest());

        assertThat(adapterWithToken("wrong-token").analyze("Great job")).isEmpty();
        server.verify();
    }

    @Test
    void returnsEmptyWhenTheApiIsDown() {
        server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

        assertThat(adapterWithToken("secret").analyze("Great job")).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheApiReturnsNoPredictions() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(adapterWithToken("secret").analyze("Great job")).isEmpty();
    }

    @Test
    void doesNotCallTheApiWithoutToken() {
        HuggingFaceSentimentAdapter adapter = adapterWithToken("");

        assertThat(adapter.isAvailable()).isFalse();
        assertThat(adapter.analyze("Great job")).isEmpty();
        server.verify(); // no request expected, none made
    }

    @Test
    void isAvailableWithAToken() {
        assertThat(adapterWithToken("secret").isAvailable()).isTrue();
    }

    @Test
    void understandsNumberedLabelsOfThreeClassModels() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
                "[[{\"label\":\"LABEL_0\",\"score\":0.88},{\"label\":\"LABEL_2\",\"score\":0.1}]]",
                MediaType.APPLICATION_JSON));

        assertThat(adapterWithToken("secret").analyze("Not good")).contains(new Sentiment(SentimentLabel.NEGATIVE, 0.88));
    }

    @Test
    void returnsEmptyForUnknownLabels() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
                "[[{\"label\":\"joy\",\"score\":0.88}]]", MediaType.APPLICATION_JSON));

        assertThat(adapterWithToken("secret").analyze("Yay")).isEmpty();
    }
}
