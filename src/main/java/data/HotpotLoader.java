package data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HotpotLoader {
    public static class Passage {
        public String title;
        public String text;
        public Passage(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    public static class HotpotExample {
        public String id;
        public String question;
        public String answer;
        public List<Passage> passages = new ArrayList<>();
        public List<String> goldSupportingTitles = new ArrayList<>();
    }

    public static List<HotpotExample> loadData(String filepath, int maxSamples) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(filepath));
        List<HotpotExample> examples = new ArrayList<>();

        int count = 0;
        for (JsonNode item : root) {
            if (count >= maxSamples) break;
            HotpotExample ex = new HotpotExample();
            ex.id = item.get("_id").asText();
            ex.question = item.get("question").asText();
            ex.answer = item.get("answer").asText();

            for (JsonNode sf : item.get("supporting_facts")) {
                ex.goldSupportingTitles.add(sf.get(0).asText());
            }

            JsonNode context = item.get("context");
            for (JsonNode c : context) {
                String title = c.get(0).asText();
                StringBuilder sb = new StringBuilder();
                for (JsonNode sent : c.get(1)) {
                    sb.append(sent.asText()).append(" ");
                }
                ex.passages.add(new Passage(title, sb.toString().trim()));
            }
            examples.add(ex);
            count++;
        }
        return examples;
    }
}