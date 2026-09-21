package io.github.fereshtehdev.joblens.analysis.adapter.out.jev;

import io.github.fereshtehdev.joblens.config.properties.JevProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Thin RestClient wrapper around Jev's HTTP API. No business logic, no resilience policy - that's JevPostingClassifier's job. */
@Component
class JevHttpClient {

    private final RestClient restClient;
    private final String model;

    JevHttpClient(RestClient.Builder restClientBuilder, JevProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
        this.model = properties.model();
    }

    JevResponse classify(String state, Map<String, Object> questions) {
        return restClient.post()
                .uri("/systemone")
                .body(new JevRequest(state, model, questions))
                .retrieve()
                .body(JevResponse.class);
    }
}
