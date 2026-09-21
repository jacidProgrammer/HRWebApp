package dev.jacid.hrApplication.infrastructure.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import dev.jacid.hrApplication.adapter.out.ai.HuggingFaceProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HuggingFaceProperties.class)
public class HuggingFaceClientConfig {

    @Bean
    RestClient huggingFaceRestClient(RestClient.Builder builder, HuggingFaceProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        RestClient.Builder client = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory);
        if (properties.hasToken()) {
            client.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token());
        }
        return client.build();
    }
}
