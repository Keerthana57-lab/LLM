package strategy;

import data.HotpotLoader.Passage;
import java.util.*;

public class ContextStrategy {

    // Simple TF-IDF / Token overlap selector
    public static List<Passage> selectTfIdf(String question, List<Passage> passages, int k) {
        Set<String> qTokens = new HashSet<>(Arrays.asList(question.toLowerCase().split("\\W+")));
        List<ScoredPassage> scored = new ArrayList<>();

        for (Passage p : passages) {
            Set<String> pTokens = Arrays.asList(p.text.toLowerCase().split("\\W+"));
            double score = 0;
            for (String qt : qTokens) {
                score += Collections.frequency(pTokens, qt);
            }
            scored.add(new ScoredPassage(p, score));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<Passage> topK = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) {
            topK.add(scored.get(i).passage);
        }
        return topK;
    }

    private static class ScoredPassage {
        Passage passage;
        double score;
        ScoredPassage(Passage p, double s) { this.passage = p; this.score = s; }
    }
}

    

