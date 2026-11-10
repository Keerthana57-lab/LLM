package com.example.context.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class Frontier {
    private static final ObjectMapper M = new ObjectMapper();
    private Frontier() {}
    private record Key(String strategy, String config) {}
    private static final class Agg {
        final List<Double> f1 = new ArrayList<>();
        double s1, s2, aux;
        void add(JsonNode r) { f1.add(r.path("f1").asDouble()); s1 += r.path("stage1_tokens").asDouble(); s2 += r.path("stage2_tokens").asDouble(); aux += r.path("auxiliary_embedding_tokens").asDouble(); }
        double meanF1() { return f1.stream().mapToDouble(x -> x).average().orElse(0); }
        double meanS1() { return s1 / f1.size(); }
        double meanS2() { return s2 / f1.size(); }
        double[] ci(long seed) {
            if (f1.size() < 2) return new double[]{meanF1(), meanF1()};
            Random random = new Random(seed);
            double[] samples = new double[1000];
            for (int b = 0; b < samples.length; b++) {
                double total = 0;
                for (int i = 0; i < f1.size(); i++) total += f1.get(random.nextInt(f1.size()));
                samples[b] = total / f1.size();
            }
            Arrays.sort(samples);
            return new double[]{samples[24], samples[974]};
        }
    }
    private record Summary(Key key, Agg agg) {}

    public static void analyze(Path raw, Path outputDir) throws Exception {
        Map<Key, Agg> groups = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(raw)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                JsonNode row = M.readTree(line);
                Key key = new Key(row.path("strategy").asText(), row.path("config").asText());
                groups.computeIfAbsent(key, k -> new Agg()).add(row);
            }
        }
        List<Summary> summaries = groups.entrySet().stream().map(e -> new Summary(e.getKey(), e.getValue())).toList();
        writeSummary(outputDir.resolve("candidate_summary.csv"), summaries);
        writePareto(outputDir.resolve("pareto_candidates.csv"), summaries);
        writeDecisions(outputDir.resolve("decision_paths.csv"), summaries);
    }

    private static void writeSummary(Path path, List<Summary> ss) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("strategy,config,n,mean_f1,ci95_low,ci95_high,mean_stage1_tokens,mean_stage2_tokens,mean_embedding_tokens\n");
            for (Summary s : ss) {
                double[] ci = s.agg.ci(42);
                w.write(String.format(Locale.ROOT, "%s,%s,%d,%.6f,%.6f,%.6f,%.2f,%.2f,%.2f%n",
                        s.key.strategy, s.key.config, s.agg.f1.size(), s.agg.meanF1(), ci[0], ci[1],
                        s.agg.meanS1(), s.agg.meanS2(), s.agg.aux / s.agg.f1.size()));
            }
        }
    }

    private static void writePareto(Path path, List<Summary> ss) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("N,strategy,config,mean_f1,effective_tokens\n");
            for (int n : new int[]{1, 5, 20, 100}) {
                for (Summary a : ss) {
                    double ca = a.agg.meanS2() + a.agg.meanS1() / n;
                    boolean dominated = false;
                    for (Summary b : ss) {
                        if (a == b) continue;
                        double cb = b.agg.meanS2() + b.agg.meanS1() / n;
                        if (b.agg.meanF1() >= a.agg.meanF1() && cb <= ca &&
                                (b.agg.meanF1() > a.agg.meanF1() || cb < ca)) { dominated = true; break; }
                    }
                    if (!dominated) w.write(String.format(Locale.ROOT, "%d,%s,%s,%.6f,%.2f%n",
                            n, a.key.strategy, a.key.config, a.agg.meanF1(), ca));
                }
            }
        }
    }

    private static void writeDecisions(Path path, List<Summary> ss) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("N,w,strategy,config,mean_f1,effective_tokens,score\n");
            for (int n : new int[]{1, 5, 20, 100}) {
                for (int wi = 0; wi <= 100; wi++) {
                    double weight = wi / 100.0, bestScore = -Double.MAX_VALUE;
                    Summary best = null; double bestCost = 0;
                    for (Summary s : ss) {
                        double cost = s.agg.meanS2() + s.agg.meanS1() / n;
                        if (cost <= 0) continue;
                        double score = weight * s.agg.meanF1() - (1.0 - weight) * Math.log(cost);
                        if (score > bestScore) { bestScore = score; best = s; bestCost = cost; }
                    }
                    if (best != null) w.write(String.format(Locale.ROOT,
                            "%d,%.2f,%s,%s,%.6f,%.2f,%.6f%n", n, weight,
                            best.key.strategy, best.key.config, best.agg.meanF1(), bestCost, bestScore));
                }
            }
        }
    }
}
