package com.example.context.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public final class ModelClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    private final String key, chatModel, embedModel, baseUrl;

    public ModelClient() {
        key = required("OPENAI_API_KEY");
        chatModel = required("CHAT_MODEL");
        embedModel = required("EMBED_MODEL");
        baseUrl = Optional.ofNullable(System.getenv("OPENAI_BASE_URL"))
                .filter(s -> !s.isBlank()).orElse("https://openrouter.ai/api/v1").replaceAll("/$", "");
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Set environment variable " + name);
        return value;
    }

    public record ChatResult(String text, int inputTokens, int outputTokens) {}
    public record EmbeddingResult(List<double[]> vectors, int inputTokens) {}

    public ChatResult chat(String system, String user, int maxOutputTokens) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", chatModel);
        body.put("temperature", 0);
        body.put("max_completion_tokens", maxOutputTokens);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", user);
        JsonNode response = post("/chat/completions", body);
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) throw new IllegalStateException("No chat choices returned: " + response);
        String text = choices.get(0).path("message").path("content").asText("");
        JsonNode usage = response.path("usage");
        return new ChatResult(text, usage.path("prompt_tokens").asInt(0), usage.path("completion_tokens").asInt(0));
    }

    public EmbeddingResult embed(List<String> texts) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", embedModel);
        ArrayNode input = body.putArray("input");
        texts.forEach(input::add);
        JsonNode response = post("/embeddings", body);
        Map<Integer, double[]> ordered = new TreeMap<>();
        for (JsonNode item : response.path("data")) {
            int index = item.path("index").asInt();
            JsonNode vector = item.path("embedding");
            double[] values = new double[vector.size()];
            for (int i = 0; i < values.length; i++) values[i] = vector.get(i).asDouble();
            ordered.put(index, values);
        }
        if (ordered.size() != texts.size()) throw new IllegalStateException("Embedding response count differs from input count");
        JsonNode usage = response.path("usage");
        return new EmbeddingResult(List.copyOf(ordered.values()), usage.path("prompt_tokens").asInt(0));
    }

    private JsonNode post(String route, ObjectNode body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + route))
                .timeout(Duration.ofSeconds(120)).header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("API HTTP " + response.statusCode() + ": " + response.body());
        return MAPPER.readTree(response.body());
    }
}
