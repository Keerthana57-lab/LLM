package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ModelClient {
    public static class LLMResponse {
        public String answer;
        public int promptTokens;
        public int completionTokens;
    }

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static LLMResponse callLLM(String prompt) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY"); // Or paste key securely
        String jsonPayload = mapper.writeValueAsString(Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.0
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());

        LLMResponse res = new LLMResponse();
        res.answer = root.path("choices").get(0).path("message").path("content").asText();
        res.promptTokens = root.path("usage").path("prompt_tokens").asInt();
        res.completionTokens = root.path("usage").path("completion_tokens").asInt();
        return res;
    }
}
