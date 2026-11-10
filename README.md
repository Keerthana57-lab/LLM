# Context Frontier (Java starter project)

Implements a HotpotQA context-strategy experiment with full context, oracle context, query-aware TF-IDF, question-independent TF-IDF, semantic embedding retrieval, and LLM compression. It logs raw per-question records, then calculates bootstrap F1 intervals, candidate summaries, Pareto candidates, and score-optimal decisions across reuse N and preference w.

## Requirements
- JDK 17+
- Maven 3.8+
- API key for an OpenAI-compatible chat and embedding API

## Setup
1. Download the HotpotQA distractor validation JSON from the official HotpotQA release and save it as `data/hotpot_dev_distractor_v1.json`.
2. Set environment variables (do not put secrets in source code):
   - `OPENAI_API_KEY`
   - `CHAT_MODEL` (exact chat model ID available to your account)
   - `EMBED_MODEL` (exact embedding model ID available to your account)
   - Optional `OPENAI_BASE_URL`, default `https://api.openai.com/v1`
3. From this directory run a tiny smoke test first:

   `mvn compile exec:java -Dexec.mainClass=com.example.contextfrontier.app.RunExperiment -Dexec.args="5"`

4. Inspect `results/raw.jsonl`, then run more examples, e.g. `20`, `200`, and only then a larger sample. The sample is shuffled with seed 42. Every model response is saved as it completes.

The request uses `/chat/completions`, `max_completion_tokens`, and `/embeddings`. Some providers use different request fields or endpoints; adjust `ModelClient.java` to that provider's API.

## Output files
- `results/raw.jsonl`: one row per example/strategy/configuration.
- `results/candidate_summary.csv`: mean F1, bootstrap 95% interval, and mean stage costs.
- `results/pareto_candidates.csv`: non-dominated candidates for each N.
- `results/decision_paths.csv`: best weighted-score candidate for each N and w.

## Important interpretation notes
- `stage1_tokens` is compression-model input+output tokens. `stage2_tokens` is answering-model input+output tokens. The paper-style metric is `stage2 + stage1 / N`.
- Semantic embedding usage is logged separately as `auxiliary_embedding_tokens`; it is not silently treated as free. For a true all-in cost comparison, convert it to provider cost or add a clearly defined cost conversion.
- HotpotQA examples generally have their own context. N > 1 is therefore a reuse scenario unless the same compressed context is genuinely shared by multiple queries. State this limitation in reports.
- Oracle is a diagnostic upper bound, not a deployable method.
- The compression prompt requests a ratio but does not hard-enforce a tokenizer-level ratio. Inspect realized token counts and refine with the tokenizer for your selected model.
- Results depend on model/provider, prompts, and the choices not fully specified in the paper. Record them in your report.
