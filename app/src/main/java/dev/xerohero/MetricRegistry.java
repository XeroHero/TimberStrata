package dev.xerohero;

import com.google.inject.Singleton;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import dev.xerohero.log.LogEntry; // 🏆 Updated import tracking hook
import java.util.concurrent.ConcurrentHashMap;

/**
 * System telemetry register tracking log frequencies, custom tags,
 * and aggregate analytics metrics across the TimberStrata pipeline.
 */
@Singleton
public class MetricRegistry {

    private final IntegerProperty totalProcessedCount = new SimpleIntegerProperty(0);
    private final ConcurrentHashMap<String, SimpleIntegerProperty> dynamicTagCounters = new ConcurrentHashMap<>();

    /**
     * Evaluates an incoming log entry to update telemetry counts.
     */
    public void analyzeAndRegister(LogEntry entry) {
        if (entry == null) return;

        // Increment baseline metrics
        totalProcessedCount.set(totalProcessedCount.get() + 1);

        // 🏆 FIX: Swapped out 'entry.loggerProperty()' for 'entry.getLevel()' or 'entry.getMessage()'
        // This ensures the compiler can resolve the method symbol cleanly.
        String level = entry.getLevel();
        if (level != null) {
            String sanitizedLevel = level.toUpperCase().trim();
            dynamicTagCounters.putIfAbsent(sanitizedLevel, new SimpleIntegerProperty(0));
            dynamicTagCounters.get(sanitizedLevel).set(dynamicTagCounters.get(sanitizedLevel).get() + 1);
        }

        // Optional: If you were searching for a logger signature string inside the message text instead:
        String message = entry.getMessage();
        if (message != null && message.contains("TRACKED_MODULE")) {
            // Your custom telemetry indexing logic here...
        }
    }

    public IntegerProperty totalProcessedCountProperty() {
        return totalProcessedCount;
    }

    public SimpleIntegerProperty getCounterForTag(String tag) {
        return dynamicTagCounters.get(tag.toUpperCase().trim());
    }
}