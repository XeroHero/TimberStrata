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
     * Sends a log trace line directly to the local Ollama instance on an asynchronous thread.
     * Returns a CompletableFuture containing the raw string response markdown summary.
     */
    public CompletableFuture<String> explainLogAsync(String rawLogLine) {
        String provider = config.getString("ai.provider", "ollama");
        String endpoint = config.getString("ai.endpoint", "http://localhost:11434/api/generate");

        // 🛠️ FIX 1: Default fallback configured directly to match your local llama3.2:3b registry
        String model = config.getString("ai.model", "llama3.2:3b");

        // Guard clause ensuring configuration layout matches
        if (!"ollama".equalsIgnoreCase(provider)) {
            return CompletableFuture.completedFuture("⚠️ Current provider is not configured for Ollama local deployments.");
        }

        // Context prompts directing the behavior of the small local LLM model
        String systemPrompt = "You are a senior DevOps engineer. Explain this server error log concisely in two sentences and give a fix: ";
        String fullPrompt = systemPrompt + rawLogLine;

        // 🛠️ FIX 2: Fully escape tabs, carriage returns, and newlines so multi-line
        // stack traces won't break the enclosing raw JSON format rules.
        String escapedPrompt = fullPrompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

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
        """.formatted(model, escapedPrompt, config.getInt("ai.max-tokens", 150));

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
                        return "❌ Ollama returned an invalid HTTP state error code: " + response.statusCode() +
                                "\nRequested Model Tag: " + model;
                    }
                })
                .exceptionally(ex -> "💥 Cannot connect to local Ollama. Is the daemon running? -> " + ex.getMessage());
    }

    /**
     * Defensive JSON extraction framework capturing both standard responses
     * and explicit Ollama API error messages without crashing the thread context.
     */
    private String parseOllamaResponse(String jsonResponseBody) {
        if (jsonResponseBody == null || jsonResponseBody.isBlank()) {
            return "⚠️ Ollama returned an empty or null payload response bundle.";
        }

        try {
            // 🛠️ FIX 3: Intercept explicit error keys if Ollama pushes back due to missing assets
            if (jsonResponseBody.contains("\"error\":")) {
                int errIndex = jsonResponseBody.indexOf("\"error\":\"");
                if (errIndex != -1) {
                    int start = errIndex + 9;
                    int end = jsonResponseBody.indexOf("\"", start);
                    if (end != -1) {
                        return "❌ Ollama Engine Error: " + jsonResponseBody.substring(start, end);
                    }
                }
                return "❌ Ollama Engine Error: " + jsonResponseBody;
            }

            // Standard payload isolation logic loop
            int targetIndex = jsonResponseBody.indexOf("\"response\":\"");
            if (targetIndex != -1) {
                int start = targetIndex + 12;
                int end = jsonResponseBody.indexOf("\"", start);

                // Track back if the string literal ended on an escaped quote \"
                while (end != -1 && jsonResponseBody.charAt(end - 1) == '\\') {
                    end = jsonResponseBody.indexOf("\"", end + 1);
                }

                if (end != -1 && end > start) {
                    return jsonResponseBody.substring(start, end)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                            .replace("\\t", "\t");
                }
            }
        } catch (Exception e) {
            System.err.println("💥 [CRITICAL PARSER CRASH] Failed to isolate JSON substrings safely:");
            e.printStackTrace();
            return "⚠️ Exception occurred parsing local response payload: " + e.getMessage();
        }

        return "⚠️ Could not extract message text. Raw Payload Context:\n" + jsonResponseBody;
    }
}