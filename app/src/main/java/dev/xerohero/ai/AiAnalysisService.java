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
        // Native, lightweight HTTP client built directly into the JDK
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Sends a raw log trace line directly to the local Ollama instance on an asynchronous thread.
     * Returns a CompletableFuture containing the raw structured string response markdown summary.
     */
    public CompletableFuture<String> explainLogAsync(String rawLogLine) {
        String provider = config.getString("ai.provider", "ollama");
        String endpoint = config.getString("ai.endpoint", "http://localhost:11434/api/generate");
        String model = config.getString("ai.model", "qwen2.5:3b");

        // Guard clause ensuring configuration layout matches
        if (!"ollama".equalsIgnoreCase(provider)) {
            return CompletableFuture.completedFuture("⚠️ Current provider is not configured for Ollama local deployments.");
        }

        // Context prompts directing the behavior of the small local LLM model
        String systemPrompt = "You are a senior DevOps engineer. Explain this server error log concisely in two sentences and give a fix: ";
        String fullPrompt = systemPrompt + rawLogLine;

        // Sanitize inner symbols to safeguard against broken JSON payloads
        String escapedPrompt = fullPrompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");

        // Ollama API request scheme ("stream": false forces a completed single text payload bundle)
        String jsonPayload = """
        {
          "model": "%s",
          "prompt": "%s",
          "stream": false,
          "options": {
            "num_predict": %d
          }
        }
        """.formatted(model, escapedPrompt, config.getInt("ai.max-tokens", 120));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Dispatches network loop processing tasks off the primary JavaFX Application Thread
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return parseOllamaResponse(response.body());
                    } else {
                        return "❌ Ollama returned an invalid HTTP state error code: " + response.statusCode();
                    }
                })
                .exceptionally(ex -> "💥 Cannot connect to local Ollama. Is the daemon running? -> " + ex.getMessage());
    }

    /**
     * Manually extracts the text within the "response" property key from the JSON payload.
     * Avoids importing thick JSON marshalling libraries for simple string extractions.
     */
    private String parseOllamaResponse(String jsonResponseBody) {
        try {
            int targetIndex = jsonResponseBody.indexOf("\"response\":\"");
            if (targetIndex != -1) {
                int start = targetIndex + 12;
                int end = jsonResponseBody.indexOf("\"", start);

                // Track back if the string literal ended on an escaped quote \"
                while (end != -1 && jsonResponseBody.charAt(end - 1) == '\\') {
                    end = jsonResponseBody.indexOf("\"", end + 1);
                }

                if (end != -1) {
                    return jsonResponseBody.substring(start, end)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                }
            }
        } catch (Exception e) {
            return "⚠️ Failed parsing local JSON response string data framework fields: " + e.getMessage();
        }
        return "⚠️ Could not parse the local response structure cleanly. Check console output strings.";
    }
}