package dev.xerohero;

import java.util.HashMap;
import java.util.Map;

public class LogParser {

    public static Map<String, String> parseLine(String line) {
        if (line == null) return null;

        String cleanLine = line.trim();
        if (cleanLine.isEmpty()) return null;

        // Split strictly by the Tab character format from our generator
        String[] parts = cleanLine.split("\\t");

        // Standard structured log row: Timestamp, Level, Logger, Message
        if (parts.length >= 4) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("timestamp", parts[0].trim());
            attributes.put("level", parts[1].trim());
            attributes.put("logger", parts[2].trim());

            // Rejoin message text fragments if tabs were nested inside the log payload string
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (i > 3) messageBuilder.append("\t");
                messageBuilder.append(parts[i]);
            }
            attributes.put("message", messageBuilder.toString().trim());
            return attributes;
        }

        // Fallback: If it's an unformatted exception stack trace line item
        if (line.startsWith("\t") || line.startsWith("    ") || cleanLine.startsWith("at ")) {
            Map<String, String> traceAttributes = new HashMap<>();
            traceAttributes.put("timestamp", "");
            traceAttributes.put("level", "ERROR");
            traceAttributes.put("logger", "StackTrace");
            traceAttributes.put("message", cleanLine);
            return traceAttributes;
        }

        return null;
    }
}