package com.example.context.strategy;

import com.example.context.data.HotpotLoader.Example;
import com.example.context.data.HotpotLoader.Passage;
import com.example.context.model.ModelClient;
import java.util.*;
import java.util.stream.*;

public final class TfidfStrategy implements ContextStrategy {
    private final boolean queryAware;
    public TfidfStrategy(boolean queryAware) { this.queryAware = queryAware; }
    public String name() { return queryAware ? "tfidf_query_aware" : "tfidf_question_independent"; }
    public List<String> configurations() { return List.of("k=3", "k=8", "k=16"); }

    public PreparedContext prepare(Example ex, String configuration, ModelClient client) {
        int k = Integer.parseInt(configuration.substring(configuration.indexOf('=') + 1));
        List<Passage> docs = ex.passages();
        int n = docs.size();
        List<List<String>> words = docs.stream().map(p -> tokens(p.text())).toList();
        Map<String, Integer> df = new HashMap<>();
        for (List<String> doc : words) for (String term : new HashSet<>(doc)) df.merge(term, 1, Integer::sum);
        List<String> query = tokens(ex.question());
        double[] scores = new double[n];
        for (int i = 0; i < n; i++) {
            if (queryAware) {
                Map<String, Integer> tf = new HashMap<>();
                for (String t : words.get(i)) tf.merge(t, 1, Integer::sum);
                for (String q : query) scores[i] += tf.getOrDefault(q, 0) * idf(q, df, n);
            } else {
                Set<String> unique = new HashSet<>(words.get(i));
                scores[i] = unique.stream().mapToDouble(t -> idf(t, df, n)).average().orElse(0.0);
            }
        }
        List<Passage> selected = IntStream.range(0, n).boxed()
                .sorted((a, b) -> Double.compare(scores[b], scores[a]))
                .limit(Math.min(k, n)).map(docs::get).toList();
        String text = selected.stream().map(p -> "Title: " + p.title() + "\n" + p.text()).collect(Collectors.joining("\n\n"));
        return new PreparedContext(text, 0, 0);
    }
    private static double idf(String t, Map<String, Integer> df, int n) {
        return Math.log((n + 1.0) / (df.getOrDefault(t, 0) + 1.0)) + 1.0;
    }
    private static List<String> tokens(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(s -> !s.isBlank()).toList();
    }
}
