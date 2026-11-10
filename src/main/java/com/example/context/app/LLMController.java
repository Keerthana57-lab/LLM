package com.example.context.app;

import com.example.context.model.ModelClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Allows your frontend to communicate with the backend seamlessly
public class LLMController {

    private final ModelClient client = new ModelClient();

    @PostMapping
    public ChatResponse askLLM(@RequestBody ChatRequest request) throws Exception {
        // Calls your existing ModelClient backend
        ModelClient.ChatResult result = client.chat(
            "You are a helpful AI assistant.",
            request.prompt(),
            500
        );

        // Return structured response with token metrics
        return new ChatResponse(
            result.text(),
            result.inputTokens(),
            result.outputTokens(),
            result.inputTokens() + result.outputTokens()
        );
    }

    // Request and Response DTO Records
    public record ChatRequest(String prompt) {}
    public record ChatResponse(String text, int inputTokens, int outputTokens, int totalTokens) {}
}