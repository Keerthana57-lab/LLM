package app;

import data.HotpotLoader;
import data.HotpotLoader.HotpotExample;
import data.HotpotLoader.Passage;
import metrics.MetricsEvaluator;
import strategy.ContextStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

public class RunExperiment {
    public static void main(String[] args) {
        System.out.println("Starting HotpotQA Experiment Pipeline...");

        try {
            // 1. Define paths and parameters
            String datasetPath = "data/hotpot_dev_distractor_v1.json";
            String outputPath = "results/raw/eval_results.jsonl";
            
            // Ensure output directory exists
            new File("results/raw").mkdirs();

            // 2. Load a pilot batch of 5 examples to test safely
            int pilotSampleSize = 5;
            System.out.println("Loading " + pilotSampleSize + " examples from dataset...");
            List<HotpotExample> examples = HotpotLoader.loadData(datasetPath, pilotSampleSize);
            System.out.println("Successfully loaded " + examples.size() + " examples.");

            // 3. Setup JSON writer for raw results (JSONL format)
            PrintWriter writer = new PrintWriter(new FileWriter(outputPath));
            ObjectMapper mapper = new ObjectMapper();

            int k = 3; // Number of passages to retrieve for TF-IDF

            // 4. Main Experiment Loop over dataset examples
            for (int i = 0; i < examples.size(); i++) {
                HotpotExample ex = examples.get(i);
                System.out.println("\n--- Processing Example " + (i + 1) + "/" + examples.size() + " [ID: " + ex.id + "] ---");

                // Apply TF-IDF strategy to select top-k passages
                List<Passage> selectedPassages = ContextStrategy.selectTfIdf(ex.question, ex.passages, k);

                // Build context string from selected passages
                StringBuilder contextBuilder = new StringBuilder();
                for (Passage p : selectedPassages) {
                    contextBuilder.append(p.title).append(": ").append(p.text).append("\n");
                }
                String preparedContext = contextBuilder.toString().trim();

                // Mock model response generation for pilot validation
                String mockPrediction = "Sample generated answer for testing pipeline.";
                int mockStage1Tokens = 500; // Preprocessing/Context tokens
                int mockStage2Tokens = 150; // Answering prompt tokens

                // Compute metrics
                double f1Score = MetricsEvaluator.computeF1(mockPrediction, ex.answer);

                // Calculate effective tokens across different reuse levels
                double effectiveTokensN1 = MetricsEvaluator.computeEffectiveTokens(mockStage2Tokens, mockStage1Tokens, 1);
                double effectiveTokensN100 = MetricsEvaluator.computeEffectiveTokens(mockStage2Tokens, mockStage1Tokens, 100);

                // 5. Log record as JSON object
                ObjectNode resultNode = mapper.createObjectNode();
                resultNode.put("id", ex.id);
                resultNode.put("question", ex.question);
                resultNode.put("gold_answer", ex.answer);
                resultNode.put("prediction", mockPrediction);
                resultNode.put("strategy", "TF-IDF");
                resultNode.put("k", k);
                resultNode.put("f1_score", f1Score);
                resultNode.put("stage1_tokens", mockStage1Tokens);
                resultNode.put("stage2_tokens", mockStage2Tokens);
                resultNode.put("effective_tokens_n1", effectiveTokensN1);
                resultNode.put("effective_tokens_n100", effectiveTokensN100);

                // Write line to JSONL file
                writer.println(mapper.writeValueAsString(resultNode));
                
                System.out.println("Question: " + ex.question);
                System.out.println("Computed F1 Score: " + f1Score);
                System.out.println("Effective Tokens (N=1): " + effectiveTokensN1);
            }

            writer.close();
            System.out.println("\nExperiment complete! Results saved to " + outputPath);

        } catch (Exception e) {
            System.err.println("Error running experiment: " + e.getMessage());
            e.printStackTrace();
        }
    }
}