package dev.xerohero;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class DashboardModel {
    private final ObservableList<LogEntry> logData;
    private final FilteredList<LogEntry> filteredLogData;
    private final SortedList<LogEntry> sortedLogData;
    private boolean autoFollowLatest = true;

    public DashboardModel(ObservableList<LogEntry> logData) {
        this.logData = logData;
        this.filteredLogData = new FilteredList<>(this.logData, p -> true);
        this.sortedLogData = new SortedList<>(filteredLogData);
    }

    public ObservableList<LogEntry> getLogData() { return logData; }
    public FilteredList<LogEntry> getFilteredLogData() { return filteredLogData; }
    public SortedList<LogEntry> getSortedLogData() { return sortedLogData; }

    public boolean isAutoFollowLatest() { return autoFollowLatest; }
    public void setAutoFollowLatest(boolean autoFollowLatest) { this.autoFollowLatest = autoFollowLatest; }
}