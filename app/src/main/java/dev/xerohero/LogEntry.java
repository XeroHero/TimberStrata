package dev.xerohero;

import javafx.beans.property.*;

public class LogEntry {
    private final StringProperty timestamp;
    private final StringProperty level;
    private final StringProperty logger;
    private final StringProperty message;
    // Add this dynamic boolean tracking state
    private final BooleanProperty marked;

    public LogEntry(String timestamp, String level, String logger, String message) {
        this.timestamp = new SimpleStringProperty(timestamp);
        this.level = new SimpleStringProperty(level);
        this.logger = new SimpleStringProperty(logger);
        this.message = new SimpleStringProperty(message);
        this.marked = new SimpleBooleanProperty(false); // Unmarked by default
    }

    public StringProperty timestampProperty() { return timestamp; }
    public StringProperty levelProperty() { return level; }
    public StringProperty loggerProperty() { return logger; }
    public StringProperty messageProperty() { return message; }

    // Add these three helper methods so DashboardApp can hook into the checkboxes
    public BooleanProperty markedProperty() { return marked; }
    public boolean isMarked() { return marked.get(); }
    public void setMarked(boolean marked) { this.marked.set(marked); }
}