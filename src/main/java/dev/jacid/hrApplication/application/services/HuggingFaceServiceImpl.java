package dev.jacid.hrApplication.application.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import dev.jacid.hrApplication.domain.model.dto.HuggingFaceResultDTO;
import reactor.core.publisher.Mono;

@Service
public class HuggingFaceServiceImpl {

    private final WebClient client;
    private final String model;

    public HuggingFaceServiceImpl(
            @Value("${huggingface.token}") String token,
            @Value("${huggingface.model}") String model
    ) {
        this.model = model;

        this.client = WebClient.builder()
                .baseUrl("https://router.huggingface.co")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }


    public Mono<HuggingFaceResultDTO> analyzeSentiment(String text) {
        return client.post()
                .uri("/hf-inference/models/" + model)
                .bodyValue(Map.of(
                        "inputs", text,
                        "wait_for_model", true   // ← Mover aquí!
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("HuggingFace error: " + body))
                )
                .bodyToMono(new ParameterizedTypeReference<List<List<HuggingFaceResultDTO>>>() {})
                .map(listOfLists -> {
                    if (listOfLists == null || listOfLists.isEmpty()) {
                        return null;
                    }
                    List<HuggingFaceResultDTO> innerList = listOfLists.get(0);
                    if (innerList == null || innerList.isEmpty()) {
                        return null;
                    }
                    return innerList.get(0); // First element with bigger score
                });
    }

}
