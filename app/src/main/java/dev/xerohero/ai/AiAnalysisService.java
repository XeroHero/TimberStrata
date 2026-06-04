package dev.xerohero.ai;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.xerohero.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

@Singleton
public class AiAnalysisService {

    private final AppConfig config;
    private final HttpClient httpClient;

    @Inject
    public AiAnalysisService(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Accepts a raw log string and returns an explanation asynchronously.
     * This prevents your JavaFX UI thread from blocking while waiting for the network response!
     */
    public CompletableFuture<String> explainLogAsync(String rawLogLine) {
        String apiKey = config.getString("ai.api.key", "");
        String model = config.getString("ai.model", "gpt-4o-mini");

        if (apiKey.isEmpty() || apiKey.startsWith("sk-")) {
            return CompletableFuture.completedFuture("⚠️ AI Configuration Error: Please provide a valid API key in config.properties.");
        }

        // Construct a clean, structured JSON payload for the endpoint chat completion
        String escapedLog = rawLogLine.replace("\"", "\\\"");
        String jsonPayload = """
        {
          "model": "%s",
          "messages": [
            {"role": "system", "content": "You are a senior DevOps and backend engineer. Explain the following error log line succinctly in 2 sentences and suggest a specific fix."},
            {"role": "user", "content": "%s"}
          ],
          "max_tokens": %d
        }
        """.formatted(model, escapedLog, config.getInt("ai.max-tokens", 150));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return parseResponseText(response.body());
                    } else {
                        return "❌ AI analysis failed. HTTP Status Code: " + response.statusCode();
                    }
                })
                .exceptionally(ex -> "💥 Error connecting to the AI provider gateway: " + ex.getMessage());
    }

    private String parseResponseText(String jsonResponseBody) {
        // Simple manual parsing or use an open-source JSON parser tool to extract:
        // choices[0].message.content
        try {
            int targetIndex = jsonResponseBody.indexOf("\"content\": \"");
            if (targetIndex != -1) {
                int start = targetIndex + 12;
                int end = jsonResponseBody.indexOf("\"", start);
                return jsonResponseBody.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
            }
        } catch (Exception e) {
            return "⚠️ Failed to extract message content out of response payload frame.";
        }
        return jsonResponseBody; // Fallback to raw output if index matching splits
    }
}
