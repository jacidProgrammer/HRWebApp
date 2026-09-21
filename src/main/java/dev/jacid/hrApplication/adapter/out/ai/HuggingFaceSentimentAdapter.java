package dev.jacid.hrApplication.adapter.out.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.domain.model.Sentiment;

/**
 * {@link SentimentAnalyzer} backed by the Hugging Face inference API.
 * Failures are logged and reported as "no sentiment" so that storing feedback never depends
 * on the availability of the external service.
 */
@Component
public class HuggingFaceSentimentAdapter implements SentimentAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceSentimentAdapter.class);

    private static final ParameterizedTypeReference<List<List<HuggingFacePrediction>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final HuggingFaceProperties properties;

    public HuggingFaceSentimentAdapter(RestClient huggingFaceRestClient, HuggingFaceProperties properties) {
        this.client = huggingFaceRestClient;
        this.properties = properties;
        if (!properties.hasToken()) {
            log.warn("HUGGINGFACE_TOKEN is not set: feedback will be stored without sentiment analysis");
        }
    }

    @Override
    public Optional<Sentiment> analyze(String text) {
        if (!properties.hasToken()) {
            return Optional.empty();
        }
        try {
            List<List<HuggingFacePrediction>> response = client.post()
                    .uri("/hf-inference/models/" + properties.model())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("inputs", text, "wait_for_model", true))
                    .retrieve()
                    .body(RESPONSE_TYPE);
            return firstPrediction(response);
        } catch (RuntimeException e) {
            log.warn("Hugging Face sentiment analysis failed, storing feedback without sentiment: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** The API returns one list per input, sorted by descending score; the first entry is the predicted label. */
    private static Optional<Sentiment> firstPrediction(List<List<HuggingFacePrediction>> response) {
        if (response == null || response.isEmpty() || response.get(0) == null || response.get(0).isEmpty()) {
            return Optional.empty();
        }
        HuggingFacePrediction best = response.get(0).get(0);
        return best == null ? Optional.empty() : Optional.of(new Sentiment(best.label(), best.score()));
    }
}
