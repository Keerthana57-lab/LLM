package metrics;

import java.util.*;

public class MetricsEvaluator {
    public static double computeF1(String prediction, String gold) {
        Set<String> predTokens = tokenize(prediction);
        Set<String> goldTokens = tokenize(gold);
        if (predTokens.isEmpty() || goldTokens.isEmpty()) return 0.0;

        int common = 0;
        for (String t : predTokens) {
            if (goldTokens.contains(t)) common++;
        }
        if (common == 0) return 0.0;

        double precision = (double) common / predTokens.size();
        double recall = (double) common / goldTokens.size();
        return (2.0 * precision * recall) / (precision + recall);
    }

    private static Set<String> tokenize(String text) {
        return new HashSet<>(Arrays.asList(text.toLowerCase().split("\\W+")));
    }

    public static double computeEffectiveTokens(int stage2Tokens, int stage1Tokens, int N) {
        return stage2Tokens + ((double) stage1Tokens / N);
    }
}