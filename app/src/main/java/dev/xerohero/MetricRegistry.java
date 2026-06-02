package dev.xerohero;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import java.util.concurrent.ConcurrentHashMap;

public class MetricRegistry {
    private final IntegerProperty errorCount = new SimpleIntegerProperty(0);
    private final ObservableMap<String, IntegerProperty> customCounters =
            FXCollections.observableMap(new ConcurrentHashMap<>());

    public IntegerProperty errorCountProperty() { return errorCount; }
    public ObservableMap<String, IntegerProperty> getCustomCounters() { return customCounters; }

    public void registerTag(String tag) {
        String upperTag = tag.trim().toUpperCase();
        if (!upperTag.isEmpty() && !"ERROR".equals(upperTag)) {
            customCounters.putIfAbsent(upperTag, new SimpleIntegerProperty(0));
        }
    }

    public void incrementError() {
        errorCount.set(errorCount.get() + 1);
    }

    public void evaluateEntry(LogEntry entry) {
        String severity = entry.levelProperty().get().toUpperCase();
        String message = entry.messageProperty().get().toUpperCase();
        String logger = entry.loggerProperty().get().toUpperCase();

        if ("ERROR".equals(severity)) {
            incrementError();
        }

        customCounters.forEach((targetTag, property) -> {
            // Strict exact evaluation on severity level; standard substring containment checking on deep message values
            if (severity.equals(targetTag) || message.contains(targetTag) || logger.contains(targetTag)) {
                property.set(property.get() + 1);
            }
        });
    }

    public void resetAll() {
        errorCount.set(0);
        customCounters.values().forEach(prop -> prop.set(0));
    }
}