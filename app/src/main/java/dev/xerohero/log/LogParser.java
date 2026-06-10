package dev.xerohero.log;

import java.util.HashMap;
import java.util.Map;

public class LogParser {

    public static Map<String, String> parseLine(String line) {
        if (line == null) return null;

        String cleanLine = line.trim();
        if (cleanLine.isEmpty()) return null;

        String[] parts = cleanLine.split("\\t");

        if (parts.length >= 4) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("timestamp", parts[0].trim());
            attributes.put("level", parts[1].trim());
            attributes.put("logger", parts[2].trim());

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (i > 3) messageBuilder.append("\t");
                messageBuilder.append(parts[i]);
            }
            attributes.put("message", messageBuilder.toString().trim());
            return attributes;
        }

        // Return null for raw traceback fragments so the Watcher groups them
        return null;
    }
}