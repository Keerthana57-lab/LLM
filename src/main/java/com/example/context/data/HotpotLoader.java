package com.example.context.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.*;

public final class HotpotLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private HotpotLoader() {}

    public record Passage(String title, String text, boolean supporting) {}
    public record Example(String id, String question, String answer, List<Passage> passages) {}

    public static List<Example> load(Path path, int limit, long seed) throws Exception {
        JsonNode root = MAPPER.readTree(path.toFile());
        List<Example> rows = new ArrayList<>();
        for (JsonNode row : root) {
            Set<String> supportTitles = new HashSet<>();
            for (JsonNode support : row.path("supporting_facts")) {
                if (support.isArray() && support.size() >= 1) supportTitles.add(support.get(0).asText());
            }
            List<Passage> passages = new ArrayList<>();
            for (JsonNode pair : row.path("context")) {
                if (!pair.isArray() || pair.size() < 2) continue;
                String title = pair.get(0).asText();
                List<String> sentences = new ArrayList<>();
                pair.get(1).forEach(s -> sentences.add(s.asText()));
                passages.add(new Passage(title, String.join(" ", sentences), supportTitles.contains(title)));
            }
            rows.add(new Example(row.path("_id").asText(), row.path("question").asText(),
                    row.path("answer").asText(), List.copyOf(passages)));
        }
        Collections.shuffle(rows, new Random(seed));
        return List.copyOf(rows.subList(0, Math.min(Math.max(limit, 0), rows.size())));
    }
}
