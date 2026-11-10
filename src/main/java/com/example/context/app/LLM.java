package com.example.context.app;

import com.example.context.model.ModelClient;
import java.util.Scanner;

public class LLM {
    public static void main(String[] args) throws Exception {
        ModelClient client = new ModelClient();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("   LIVE LLM INTERACTIVE TERMINAL READY");
        System.out.println("   Type your question and press Enter.");
        System.out.println("   Type 'exit' or 'quit' to end the session.");
        System.out.println("==================================================\n");

        while (true) {
            System.out.print("\nUser > ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting session. Goodbye!");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            System.out.println("\n[LLM is thinking...]");
            try {
                ModelClient.ChatResult result = client.chat(
                    "You are a helpful AI assistant.",
                    input,
                    500
                );

                System.out.println("\n------------------ LLM RESPONSE ------------------");
                System.out.println(result.text());
                System.out.println("--------------------------------------------------");
                
                // Displaying token counts
                int promptTokens = result.inputTokens();
                int completionTokens = result.outputTokens();
                int totalTokens = promptTokens + completionTokens;
                System.out.println("[Tokens Used -> Input: " + promptTokens + " | Output: " + completionTokens + " | Total: " + totalTokens + "]");
                
            } catch (Exception e) {
                System.out.println("Error fetching response: " + e.getMessage());
            }
        }
        scanner.close();
    }
}