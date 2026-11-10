package com.example.context.strategy;

import com.example.context.data.HotpotLoader.Example;
import com.example.context.data.HotpotLoader.Passage;
import com.example.context.model.ModelClient;
import java.util.*;
import java.util.stream.*;

public final class SemanticStrategy implements ContextStrategy {
    public String name() { return "semantic_embedding"; }
    public List<String> configurations() { return List.of("k=3", "k=8", "k=16"); }

    public PreparedContext prepare(Example ex, String configuration, ModelClient client) throws Exception {
        int k = Integer.parseInt(configuration.substring(configuration.indexOf('=') + 1));
        List<String> inputs = new ArrayList<>();
        inputs.add(ex.question());
        ex.passages().forEach(p -> inputs.add(p.text()));
        ModelClient.EmbeddingResult result = client.embed(inputs);
        List<double[]> vectors = result.vectors();
        double[] q = vectors.get(0);
        double[] scores = new double[ex.passages().size()];
        for (int i = 0; i < scores.length; i++) scores[i] = cosine(q, vectors.get(i + 1));
        List<Passage> selected = IntStream.range(0, scores.length).boxed()
                .sorted((a, b) -> Double.compare(scores[b], scores[a]))
                .limit(Math.min(k, scores.length)).map(ex.passages()::get).toList();
        String text = selected.stream().map(p -> "Title: " + p.title() + "\n" + p.text()).collect(Collectors.joining("\n\n"));
        return new PreparedContext(text, 0, result.inputTokens());
    }
    private static double cosine(double[] a, double[] b) {
        double dot = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i]; }
        return dot / (Math.sqrt(aa) * Math.sqrt(bb) + 1e-12);
    }
}
