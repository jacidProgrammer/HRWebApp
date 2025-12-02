package dev.jacid.hrApplication.application.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.jacid.hrApplication.domain.model.dto.HuggingFaceResult;
import reactor.core.publisher.Mono;

@Service
public class HuggingFaceService {

    private final WebClient client;
    private final String model;

    public HuggingFaceService(
            @Value("${huggingface.token}") String token,
            @Value("${huggingface.model}") String model
    ) {
        this.model = model;
        this.client = WebClient.builder()
                .baseUrl("https://api-inference.huggingface.co/models")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    public Mono<List<HuggingFaceResult>> analyzeSentiment(String text) {
        return client.post()
                .uri("/" + model)
                .bodyValue(Map.of("inputs", text))
                .retrieve()
                .bodyToFlux(HuggingFaceResult.class)  
                .collectSortedList();
    }
}
