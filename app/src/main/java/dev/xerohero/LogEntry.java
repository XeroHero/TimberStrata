package dev.xerohero;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LogEntry {
    private final StringProperty timestamp = new SimpleStringProperty();
    private final StringProperty level = new SimpleStringProperty();
    private final StringProperty logger = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();

    public LogEntry(String timestamp, String level, String logger, String message) {
        setTimestamp(timestamp);
        setLevel(level);
        setLogger(logger);
        setMessage(message);
    }

    public StringProperty timestampProperty() { return timestamp; }
    public StringProperty levelProperty() { return level; }
    public StringProperty loggerProperty() { return logger; }
    public StringProperty messageProperty() { return message; }

    public void setTimestamp(String value) { timestamp.set(value); }
    public void setLevel(String value) { level.set(value); }
    public void setLogger(String value) { logger.set(value); }
    public void setMessage(String value) { message.set(value); }
}