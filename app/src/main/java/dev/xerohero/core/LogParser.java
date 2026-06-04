package dev.xerohero.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class LogParser {

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+\\[(?<level>[A-Z]+)\\]\\s+\\[(?<logger>[a-zA-Z0-9_.-]+)\\]\\s+(?<message>.*)$"
    );

    public static Map<String, String> parseLine(String logLine) {
        Matcher matcher = LOG_PATTERN.matcher(logLine);
        if (matcher.matches()) {
            Map<String, String> parsedLog = new HashMap<>();
            parsedLog.put("timestamp", matcher.group("timestamp"));
            parsedLog.put("level", matcher.group("level"));
            parsedLog.put("logger", matcher.group("logger"));
            parsedLog.put("message", matcher.group("message"));
            return parsedLog;
        }
        return null;
    }
}
