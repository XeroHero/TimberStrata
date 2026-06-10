package dev.xerohero.log;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Data architecture model representing an isolated logged trace segment.
 * Uses JavaFX properties for reactive UI binding alongside native POJO string getters.
 */
public class LogEntry {

    private final StringProperty timestamp;
    private final StringProperty level;
    private final StringProperty message;

    public LogEntry(String timestamp, String level, String message) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.level = new SimpleStringProperty(level);
        this.message = new SimpleStringProperty(message);
    }

    // --- JavaFX Property Objects (For Column Bindings) ---
    public StringProperty timestampProperty() { return timestamp; }
    public StringProperty levelProperty() { return level; }
    public StringProperty messageProperty() { return message; }

    // --- Standard String Getters (🏆 FIXES PRESENTER COMPILATION ERRORS) ---
    public String getTimestamp() { return timestamp.get(); }
    public String getLevel() { return level.get(); }
    public String getMessage() { return message.get(); }
}