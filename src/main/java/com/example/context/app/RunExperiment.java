package com.example.context.app;

import com.example.context.data.HotpotLoader;
import com.example.context.data.HotpotLoader.Example;
import com.example.context.metrics.F1;
import com.example.context.metrics.Frontier;
import com.example.context.model.ModelClient;
import com.example.context.strategy.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class RunExperiment {
    private static final ObjectMapper M = new ObjectMapper();
    private RunExperiment() {}

    public static void main(String[] args) throws Exception {
        int limit = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        Path data = Path.of("data/hotpot_dev_distractor_v1.json");
        Path results = Path.of("results");
        Files.createDirectories(results);
        List<Example> examples = HotpotLoader.load(data, limit, 42);
        ModelClient client = new ModelClient();

        List<ContextStrategy> strategies = List.of(
                new BaselineStrategy(false), new BaselineStrategy(true),
                new TfidfStrategy(true), new TfidfStrategy(false),
                new SemanticStrategy(), new CompressionStrategy());

        Path raw = results.resolve("raw.jsonl");
        try (BufferedWriter out = Files.newBufferedWriter(raw)) {
            for (Example ex : examples) {
                System.out.println("Example " + ex.id());
                for (ContextStrategy strategy : strategies) {
                    for (String config : strategy.configurations()) {
                        ContextStrategy.PreparedContext prepared = strategy.prepare(ex, config, client);
                        ModelClient.ChatResult answer = client.chat(
                                "Answer the question using only the supplied context. Return only the answer.",
                                "CONTEXT:\n" + prepared.text() + "\n\nQUESTION:\n" + ex.question(), 200);
                        ObjectNode row = M.createObjectNode();
                        row.put("id", ex.id());
                        row.put("strategy", strategy.name());
                        row.put("config", config);
                        row.put("prediction", answer.text());
                        row.put("gold", ex.answer());
                        row.put("f1", F1.score(answer.text(), ex.answer()));
                        row.put("stage1_tokens", prepared.stage1Tokens());
                        row.put("stage2_tokens", answer.inputTokens() + answer.outputTokens());
                        row.put("auxiliary_embedding_tokens", prepared.auxiliaryTokens());
                        out.write(M.writeValueAsString(row));
                        out.newLine();
                        out.flush();
                    }
                }
            }
        }
        Frontier.analyze(raw, results);
        System.out.println("Done. See results/ for JSONL and CSV files.");
    }
}
