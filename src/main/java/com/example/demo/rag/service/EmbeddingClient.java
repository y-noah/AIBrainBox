package com.example.demo.rag.service;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EmbeddingClient {
    private String baseUrl = "http://127.0.0.1:11434";

    public List<Float> embed(String text) {
        String url = baseUrl + "/api/embeddings";

        Map<String, String> request = new HashMap<>();
        request.put("model", "nomic-embed-text");
        request.put("prompt", text);

        Map response = restTemplate.postForObject(url, request, Map.class);
        List<Double> embedding = (List<Double>) response.get("embedding");

        // 转成Float列表
        List<Float> collect = embedding.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());

        return collect;
    }

    private final RestTemplate restTemplate = new RestTemplate(
            Collections.singletonList(new MappingJackson2HttpMessageConverter())
    );
}
