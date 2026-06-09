package com.psa.capstone.be.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class GitLabConfig {
    @Value("${gitlab.api.url}")
    private String gitlabApiUrl;

    @Value("${gitlab.api.private-token}")
    private String privateToken;

    @Value("${gitlab.webhook.secret}")
    private String webhookSecret;

    @Bean
    public ObjectMapper gitlabObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // This will handle converting GitLab's snake_case to your DTO's camelCase
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    @Bean
    public WebClient gitlabWebClient(ObjectMapper gitlabObjectMapper) {
        return WebClient.builder()
                .baseUrl(gitlabApiUrl)
                .defaultHeader("PRIVATE-TOKEN", privateToken)
                .codecs(configurer -> {
                    Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(gitlabObjectMapper);
                    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(gitlabObjectMapper);
                    configurer.defaultCodecs().jackson2JsonDecoder(decoder);
                    configurer.defaultCodecs().jackson2JsonEncoder(encoder);
                })
                .build();
    }
}
