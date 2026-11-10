package com.example.context.strategy;

import com.example.context.data.HotpotLoader.Example;
import com.example.context.model.ModelClient;
import java.util.List;

public interface ContextStrategy {
    String name();
    List<String> configurations();
    PreparedContext prepare(Example example, String configuration, ModelClient client) throws Exception;

    record PreparedContext(String text, int stage1Tokens, int auxiliaryTokens) {}
}
