package dev.xerohero.ui;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance regex tokenization engine for log message colorization.
 * Auto-detects patterns like UUIDs, URLs, and IP addresses to render stylized Text blocks.
 */
public class LogStyleEngine {

    // Regex Blueprints matching key engineering identifiers
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(?<UUID>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})" +
                    "|(?<URL>https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])" +
                    "|(?<IP>\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)"
    );

    /**
     * Parses a raw string message slice and wraps tokenized segments into a stylized layout container.
     */
    public static TextFlow tokenizeAndColorize(String message) {
        TextFlow textFlow = new TextFlow();
        if (message == null || message.isEmpty()) {
            return textFlow;
        }

        Matcher matcher = LOG_PATTERN.matcher(message);
        int lastIdx = 0;

        while (matcher.find()) {
            // Append standard un-matched plain text segment leading up to the token match
            if (matcher.start() > lastIdx) {
                Text plainText = new Text(message.substring(lastIdx, matcher.start()));
                plainText.setStyle("-fx-fill: #2c3e50;"); // Deep slate charcoal
                textFlow.getChildren().add(plainText);
            }

            // Extract matching target sub-group arrays
            if (matcher.group("UUID") != null) {
                Text uuidText = new Text(matcher.group("UUID"));
                uuidText.setStyle("-fx-fill: #9b59b6; -fx-font-weight: bold;"); // Deep Amethyst Purple
                textFlow.getChildren().add(uuidText);
            } else if (matcher.group("URL") != null) {
                Text urlText = new Text(matcher.group("URL"));
                urlText.setStyle("-fx-fill: #2980b9; -fx-underline: true;"); // Electric Blue Link
                textFlow.getChildren().add(urlText);
            } else if (matcher.group("IP") != null) {
                Text ipText = new Text(matcher.group("IP"));
                ipText.setStyle("-fx-fill: #16a085; -fx-font-weight: bold;"); // Deep Teal Marine
                textFlow.getChildren().add(ipText);
            }

            lastIdx = matcher.end();
        }

        // Append any remaining tailing line segments safely
        if (lastIdx < message.length()) {
            Text tailText = new Text(message.substring(lastIdx));
            tailText.setStyle("-fx-fill: #2c3e50;");
            textFlow.getChildren().add(tailText);
        }

        return textFlow;
    }
}