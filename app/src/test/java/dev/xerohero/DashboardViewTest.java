package dev.xerohero;

import dev.xerohero.core.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardViewTest {

    private ObservableList<LogEntry> logDataMock;
    private MetricRegistry metricsMock;
    private LogDirectoryWatcher watcherMock;
    private DockerEngineManager dockerMock;
    private DashboardView dashboardView;

    @BeforeEach
    void setUp() {
        // Generate isolated framework mock attachments
        logDataMock = FXCollections.observableArrayList();
        metricsMock = mock(MetricRegistry.class);
        watcherMock = mock(LogDirectoryWatcher.class);
        dockerMock = mock(DockerEngineManager.class);

        // Guice constructor injection mimicking: perfectly decoupled execution test track
        dashboardView = new DashboardView(logDataMock, metricsMock, watcherMock, dockerMock);
    }

    @Test
    @DisplayName("Should successfully construct the view via dependency parameters without instantiation failures")
    void testViewArchitectureConstruction() {
        assertNotNull(dashboardView, "Dashboard view container should instantiate cleanly via injected contexts.");
    }
}
