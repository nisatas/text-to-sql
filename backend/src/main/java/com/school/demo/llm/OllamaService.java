package com.school.demo.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OllamaService {

    private final RestClient restClient;

    @Value("${ollama.model}")
    private String ollamaModel;

    @Value("${ollama.sql.num-predict:256}")
    private int sqlNumPredict;

    @Value("${ollama.summary.num-predict:256}")
    private int summaryNumPredict;

    public OllamaService(@Value("${ollama.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String generateSql(String prompt) {
        return generate(prompt, sqlNumPredict);
    }

    public String generateSummary(String prompt) {
        return generate(prompt, summaryNumPredict);
    }

    private String generate(String prompt, int numPredict) {
        Map<String, Object> requestBody = Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "num_predict", Math.max(64, numPredict)
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("response") == null) {
            throw new IllegalArgumentException("Ollama geçerli bir cevap döndürmedi.");
        }

        return response.get("response").toString();
    }
}