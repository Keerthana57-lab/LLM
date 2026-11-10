package com.example.context.metrics;

import java.util.*;

public final class F1 {
    private F1() {}
    public static double score(String prediction, String gold) {
        List<String> p = normalize(prediction), g = normalize(gold);
        if (p.isEmpty() || g.isEmpty()) return p.equals(g) ? 1.0 : 0.0;
        Map<String, Integer> counts = new HashMap<>();
        for (String t : g) counts.merge(t, 1, Integer::sum);
        int overlap = 0;
        for (String t : p) {
            int c = counts.getOrDefault(t, 0);
            if (c > 0) { overlap++; counts.put(t, c - 1); }
        }
        if (overlap == 0) return 0.0;
        double precision = (double) overlap / p.size(), recall = (double) overlap / g.size();
        return 2 * precision * recall / (precision + recall);
    }
    private static List<String> normalize(String s) {
        String x = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\b(a|an|the)\\b", " ").trim();
        if (x.isEmpty()) return List.of();
        return Arrays.stream(x.split("\\s+")).filter(t -> !t.isBlank()).toList();
    }
}
