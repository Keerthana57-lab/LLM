package com.example.context.strategy;

import com.example.context.data.HotpotLoader.Example;
import com.example.context.data.HotpotLoader.Passage;
import com.example.context.model.ModelClient;
import java.util.List;
import java.util.stream.Collectors;

public final class BaselineStrategy implements ContextStrategy {
    private final boolean oracle;
    public BaselineStrategy(boolean oracle) { this.oracle = oracle; }
    public String name() { return oracle ? "oracle" : "full_context"; }
    public List<String> configurations() { return List.of(oracle ? "supporting_only" : "all_passages"); }
    public PreparedContext prepare(Example example, String configuration, ModelClient client) {
        List<Passage> chosen = oracle ? example.passages().stream().filter(Passage::supporting).toList() : example.passages();
        String text = chosen.stream().map(p -> "Title: " + p.title() + "\n" + p.text()).collect(Collectors.joining("\n\n"));
        return new PreparedContext(text, 0, 0);
    }
}

