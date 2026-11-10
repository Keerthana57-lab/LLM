package com.example.context.strategy;

import com.example.context.data.HotpotLoader.Example;
import com.example.context.data.HotpotLoader.Passage;
import com.example.context.model.ModelClient;
import java.util.List;
import java.util.stream.Collectors;

public final class CompressionStrategy implements ContextStrategy {
    public String name() { return "memory_compression"; }
    public List<String> configurations() { return List.of("ratio=2.0", "ratio=2.5"); }

    public PreparedContext prepare(Example ex, String configuration, ModelClient client) throws Exception {
        double ratio = Double.parseDouble(configuration.substring(configuration.indexOf('=') + 1));
        String source = ex.passages().stream().map(p -> "Title: " + p.title() + "\n" + p.text())
                .collect(Collectors.joining("\n\n"));
        ModelClient.ChatResult result = client.chat(
                "Compress context for future questions. Preserve factual details, names, numbers, and relationships. " +
                        "Do not answer any question. Output only the compressed memory.",
                "Aim for approximately " + ratio + " times shorter than the original.\n\nSOURCE CONTEXT:\n" + source,
                1200);
        int stage1 = result.inputTokens() + result.outputTokens();
        return new PreparedContext(result.text(), stage1, 0);
    }
}
