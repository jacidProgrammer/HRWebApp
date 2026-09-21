package dev.jacid.hrApplication.adapter.out.ai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the Hugging Face inference API ({@code huggingface.*}).
 * The token is read from the {@code HUGGINGFACE_TOKEN} environment variable; when it is empty,
 * sentiment analysis is disabled and feedback is stored without a sentiment.
 */
@ConfigurationProperties(prefix = "huggingface")
public record HuggingFaceProperties(
        @DefaultValue("https://router.huggingface.co") String baseUrl,
        String token,
        @DefaultValue("cardiffnlp/twitter-roberta-base-sentiment-latest") String model,
        @DefaultValue("30s") Duration timeout) {

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }
}
