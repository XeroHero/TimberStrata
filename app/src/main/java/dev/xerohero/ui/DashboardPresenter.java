package dev.xerohero.ui;

import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import dev.xerohero.DockerEngineManager;
import dev.xerohero.log.LogEntry;
import dev.xerohero.log.LogDirectoryWatcher;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The Central Orchestrator for the TimberStrata UI Panel.
 * Coordinates reactive data streams, attaches action listeners, and fires infrastructure commands.
 */
public class DashboardPresenter {

    private final DashboardView view;
    private final DashboardModel model;
    private final LogDirectoryWatcher logWatcher;
    private final ObservableList<LogEntry> rawLogBuffer;
    private final DockerEngineManager dockerManager;

    private boolean autoFollowEnabled = true;

    @Inject
    public DashboardPresenter(DashboardView view,
                              DashboardModel model,
                              LogDirectoryWatcher logWatcher,
                              ObservableList<LogEntry> rawLogBuffer,
                              DockerEngineManager dockerManager) {
        this.view = view;
        this.model = model;
        this.logWatcher = logWatcher;
        this.rawLogBuffer = rawLogBuffer;
        this.dockerManager = dockerManager;
    }

    /**
     * Binds internal application state models to visual UI components.
     * Hooks layout event listeners and setups the rendering pipeline wrapper tree.
     */
    public void bindView(Stage stage) {
        FilteredList<LogEntry> filteredData = new FilteredList<>(rawLogBuffer, p -> true);
        SortedList<LogEntry> sortedData = new SortedList<>(filteredData);

        view.initializeLayout(sortedData);

        sortedData.comparatorProperty().bind(view.getTable().comparatorProperty());

        // Setup baseline default light theme style on load
        applyStyleTheme(stage, "/styles/light.css");

        setupSearchEnginePredicate(filteredData);
        setupActionHandlers(stage);
        setupAutoScrollFollowPipeline();
        setupDockerStatusTracker();
    }

    /**
     * Replaces the global stage style sheet with a targeted look.
     */
    private void applyStyleTheme(Stage stage, String path) {
        if (stage.getScene() != null) {
            stage.getScene().getStylesheets().clear();
            String externalUrl = getClass().getResource(path).toExternalForm();
            stage.getScene().getStylesheets().add(externalUrl);
        }
    }

    private void setupDockerStatusTracker() {
        updateDockerStatusUI(dockerManager.isRunning());
        dockerManager.runningProperty().addListener((observable, oldValue, isRunning) -> {
            Platform.runLater(() -> updateDockerStatusUI(isRunning));
        });
    }

    private void updateDockerStatusUI(boolean isRunning) {
        if (isRunning) {
            view.getDockerStatusLabel().setText("🟢 RUNNING");
            view.getDockerStatusLabel().setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
        } else {
            view.getDockerStatusLabel().setText("🔴 STOPPED");
            view.getDockerStatusLabel().setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
    }

    private void setupActionHandlers(Stage stage) {
        // 🏆 Dynamic Style Toggle Action Hook
        view.getThemeToggleBtn().setOnAction(event -> {
            if (view.getThemeToggleBtn().isSelected()) {
                view.getThemeToggleBtn().setText("☀️ Light Mode");
                applyStyleTheme(stage, "/styles/dark.css");
            } else {
                view.getThemeToggleBtn().setText("🌙 Dark Mode");
                applyStyleTheme(stage, "/styles/light.css");
            }
        });

        view.getChooseFolderBtn().setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Active Target Log Directory");
            File defaultDir = new File(System.getProperty("user.home") + "/Git/TimberStrata");
            if (defaultDir.exists()) {
                directoryChooser.setInitialDirectory(defaultDir);
            }
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                view.getActiveFileLabel().setText("Monitoring: " + selectedDirectory.getName());
                logWatcher.setTargetDirectory(selectedDirectory);
            }
        });

        view.getToggleScrollBtn().setOnAction(event -> {
            autoFollowEnabled = !autoFollowEnabled;
            if (autoFollowEnabled) {
                view.getToggleScrollBtn().setText("🔄 Auto-Follow: ON");
                view.getToggleScrollBtn().setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                forceTableViewportToBottom();
            } else {
                view.getToggleScrollBtn().setText("⏸️ Auto-Follow: OFF");
                view.getToggleScrollBtn().setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        });

        view.getManualInspectBtn().setOnAction(event -> triggerManualFrameSelectionInspection());

        view.getAddTagBtn().setOnAction(event -> {
            String targetToken = view.getCustomTagField().getText();
            if (targetToken != null && !targetToken.strip().isEmpty()) {
                String sanitizedToken = targetToken.trim().toUpperCase();
                model.registerCustomMetricKey(sanitizedToken);
                view.renderCustomMetricCard(sanitizedToken, 0, (observable, oldValue, newValue) -> {
                    Platform.runLater(() -> System.out.println("⚡ [TELEMETRY] Tag " + sanitizedToken + " counter tick: " + newValue));
                });
                view.getCustomTagField().clear();
            }
        });

        view.getRestartContainerBtn().setOnAction(event -> {
            dockerManager.restartContainer("timberstrata-core-service");
        });

        view.getStopContainerBtn().setOnAction(event -> {
            dockerManager.stopContainer("timberstrata-core-service");
        });
    }

    private void setupSearchEnginePredicate(FilteredList<LogEntry> filteredData) {
        view.getSearchField().textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(logEntry -> {
                if (newValue == null || newValue.strip().isEmpty()) return true;
                String searchToken = newValue.toLowerCase();

                if (view.getRegexToggle().isSelected()) {
                    try {
                        Pattern pattern = Pattern.compile(newValue, Pattern.CASE_INSENSITIVE);
                        return pattern.matcher(logEntry.getMessage()).find() || pattern.matcher(logEntry.getLevel()).find();
                    } catch (PatternSyntaxException e) {
                        return true;
                    }
                }
                return logEntry.getMessage().toLowerCase().contains(searchToken) || logEntry.getLevel().toLowerCase().contains(searchToken);
            });
            if (autoFollowEnabled) forceTableViewportToBottom();
        });
    }

    private void setupAutoScrollFollowPipeline() {
        rawLogBuffer.addListener((ListChangeListener<LogEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded() && autoFollowEnabled) {
                    forceTableViewportToBottom();
                }
                if (change.wasAdded()) {
                    for (LogEntry entry : change.getAddedSubList()) {
                        String severity = entry.getLevel();
                        if (severity != null && ("ERROR".equalsIgnoreCase(severity) || "FATAL".equalsIgnoreCase(severity))) {
                            model.incrementErrorTelemetryCount();
                            int currentErrors = model.getErrorTelemetryCount();
                            Platform.runLater(() -> view.getErrorCountLabel().setText("🚨 Errors: " + currentErrors));
                        }
                    }
                }
            }
        });
    }

    private void forceTableViewportToBottom() {
        Platform.runLater(() -> {
            int totalRows = view.getTable().getItems().size();
            if (totalRows > 0) {
                view.getTable().scrollTo(totalRows - 1);
            }
        });
    }

    private void triggerManualFrameSelectionInspection() {
        LogEntry selectedEntry = view.getTable().getSelectionModel().getSelectedItem();
        if (selectedEntry != null) {
            System.out.println("🔍 [INSPECTOR] Tracing context for timestamp: " + selectedEntry.getTimestamp());
        }
    }
}