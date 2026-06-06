package dev.xerohero;

import com.google.inject.Inject;
import dev.xerohero.ai.AiAnalysisService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.util.Random;

public class DashboardPresenter {

    private final DashboardView view;
    private final DashboardModel model;
    private final MetricRegistry metrics;
    private final LogDirectoryWatcher watcher;
    private final AiAnalysisService aiService;
    private final AppConfig config;

    private final String[] thinkingPhrases = {
            "Analyzing architectural life choices...", "Inverting binary trees in memory...",
            "Reticulating telemetry splines...", "Blaming the network configuration...",
            "Consulting stack overflow archives...", "Deeply contemplating garbage collection overhead...",
            "Aggressively parsing kernel panic states...", "Applying microservice psychological counseling...",
            "Re-indexing local container alignment space..."
    };

    @Inject
    public DashboardPresenter(DashboardView view, MetricRegistry metrics, LogDirectoryWatcher watcher,
                              DockerEngineManager dockerManager, AiAnalysisService aiService, AppConfig config,
                              ObservableList<LogEntry> rawLogs) {
        this.view = view;
        this.model = new DashboardModel(rawLogs);
        this.metrics = metrics;
        this.watcher = watcher;
        this.aiService = aiService;
        this.config = config;
    }

    public void bindView(Stage stage) {
        view.initializeLayout(model.getSortedLogData());
        model.getSortedLogData().comparatorProperty().bind(view.getTable().comparatorProperty());

        // --- Wiring Event Actions ---
        view.getChooseFolderBtn().setOnAction(e -> handleDirectorySelection(stage));
        view.getToggleScrollBtn().setOnAction(e -> toggleAutoFollow());
        view.getManualInspectBtn().setOnAction(e -> openInspectorPopup(view.getTable().getSelectionModel().getSelectedItem()));
        view.getAddTagBtn().setOnAction(e -> metrics.registerTag(view.getCustomTagField().getText()));

        // --- Filters & Pipeline Listeners ---
        view.getSearchField().textProperty().addListener((obs, old, nv) -> updateFilterPredicate(nv));
        metrics.errorCountProperty().addListener((obs, old, nv) -> Platform.runLater(() -> view.getErrorCountLabel().setText("🚨 Errors: " + nv)));

        // --- Custom Counters Layout Mapping ---
        metrics.getCustomCounters().addListener((MapChangeListener<String, javafx.beans.property.IntegerProperty>) change -> {
            if (change.wasAdded()) {
                String tag = change.getKey();
                var valueProp = change.getValueAdded();
                Platform.runLater(() -> view.renderCustomMetricCard(tag, valueProp.get(), (o, oldVal, newVal) ->
                        Platform.runLater(() -> ((Label)o).setText("🏷️ " + tag + ": " + newVal))
                ));
            }
        });

        // --- Table Actions ---
        setupRowFactory();
        setupKeyboardShortcuts();
        setupLogDataStreamingListener();
    }

    private void handleDirectorySelection(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            model.getLogData().clear();
            metrics.resetAll();
            view.getActiveFileLabel().setText("Monitoring: " + selected.getName());
            watcher.changeWatchedDirectory(selected);
        }
    }

    private void toggleAutoFollow() {
        model.setAutoFollowLatest(!model.isAutoFollowLatest());
        if (model.isAutoFollowLatest()) {
            view.getToggleScrollBtn().setText("🔄 Auto-Follow: ON");
            view.getToggleScrollBtn().setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
            scrollToBottom();
        } else {
            view.getToggleScrollBtn().setText("🛑 Auto-Follow: OFF");
            view.getToggleScrollBtn().setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    private void openInspectorPopup(LogEntry selected) {
        if (selected == null) return;
        if (model.isAutoFollowLatest()) toggleAutoFollow();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Log Entry Inspector");
            alert.setHeaderText("Detailed Unabridged Log Message View:");

            TextArea textArea = new TextArea(selected.messageProperty().get());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefWidth(650);
            textArea.setPrefHeight(350);
            textArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

            Button aiAnalyzeBtn = new Button("🤖 Explain with TimberAI");
            aiAnalyzeBtn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold;");
            aiAnalyzeBtn.setMaxWidth(Double.MAX_VALUE);
            aiAnalyzeBtn.setOnAction(e -> triggerAiPipeline(selected, alert));

            VBox wrapper = new VBox(10, textArea, aiAnalyzeBtn);
            VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
            wrapper.setPadding(new Insets(5));

            alert.getDialogPane().setContent(wrapper);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }

    private void triggerAiPipeline(LogEntry log, Alert containerAlert) {
        String logString = String.format("[%s] [%s] %s", log.timestampProperty().get(), log.levelProperty().get(), log.messageProperty().get());

        ProgressIndicator spinner = new ProgressIndicator(-1.0);
        spinner.setPrefSize(40, 40);
        Label loadLabel = new Label("Initializing local tracking pipelines...");
        loadLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #34495e;");

        VBox inlineLoadBox = new VBox(10, spinner, loadLabel);
        inlineLoadBox.setAlignment(Pos.CENTER);
        inlineLoadBox.setPadding(new Insets(20));

        containerAlert.getDialogPane().setContent(inlineLoadBox);
        containerAlert.setHeaderText("Thinking...");

        Random rand = new Random();
        Timeline inlineTimeline = new Timeline(new KeyFrame(Duration.seconds(1.2), tEvent ->
                loadLabel.setText(thinkingPhrases[rand.nextInt(thinkingPhrases.length)])));
        inlineTimeline.setCycleCount(Animation.INDEFINITE);
        inlineTimeline.play();

        aiService.explainLogAsync(logString)
                .thenAccept(explanation -> Platform.runLater(() -> {
                    inlineTimeline.stop();
                    containerAlert.setHeaderText("TimberAI Diagnostic Analysis Result");
                    TextArea resultArea = new TextArea(explanation);
                    resultArea.setEditable(false);
                    resultArea.setWrapText(true);
                    resultArea.setPrefWidth(650);
                    resultArea.setPrefHeight(350);
                    resultArea.setStyle("-fx-font-family: 'Helvetica Neue', Arial; -fx-font-size: 13px;");
                    containerAlert.getDialogPane().setContent(resultArea);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        inlineTimeline.stop();
                        containerAlert.setHeaderText("AI Error Trace");
                        containerAlert.getDialogPane().setContent(new Label("❌ Analysis failed."));
                    });
                    return null;
                });
    }

    private void setupRowFactory() {
        view.getTable().setRowFactory(tv -> {
            TableRow<LogEntry> row = new TableRow<>() {
                private final Tooltip stackTraceTooltip = new Tooltip();
                @Override
                protected void updateItem(LogEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    setTooltip(null);
                    if (empty || item == null) { setStyle(""); return; }
                    if (item.isMarked()) setStyle("-fx-background-color: #e8daef; -fx-font-weight: bold;");
                    else if ("ERROR".equals(item.levelProperty().get())) setStyle("-fx-background-color: #fce4d6;");
                    else if ("WARN".equals(item.levelProperty().get())) setStyle("-fx-background-color: #fef9e7;");
                    else setStyle("");

                    String msg = item.messageProperty().get();
                    if (msg != null && msg.contains("\n")) {
                        stackTraceTooltip.setText(msg);
                        stackTraceTooltip.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: #ecf0f1;");
                        setTooltip(stackTraceTooltip);
                    }
                }
            };

            ContextMenu contextMenu = new ContextMenu();
            MenuItem explainItem = new MenuItem("🤖 Explain with TimberAI");
            explainItem.setOnAction(e -> openInspectorPopup(row.getItem()));
            contextMenu.getItems().add(explainItem);

            row.itemProperty().addListener((obs, old, nv) -> row.setContextMenu(nv != null ? contextMenu : null));
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2) openInspectorPopup(row.getItem());
            });
            return row;
        });
    }

    private void updateFilterPredicate(String value) {
        model.getFilteredLogData().setPredicate(entry -> {
            if (value == null || value.trim().isEmpty()) return true;
            String lower = value.toLowerCase().trim();
            return entry.messageProperty().get().toLowerCase().contains(lower) ||
                    entry.levelProperty().get().toLowerCase().contains(lower);
        });
    }

    private void setupKeyboardShortcuts() {
        view.getTable().setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE) {
                openInspectorPopup(view.getTable().getSelectionModel().getSelectedItem());
                e.consume();
            }
        });
    }

    private void setupLogDataStreamingListener() {
        model.getLogData().addListener((javafx.collections.ListChangeListener<LogEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    int maxRows = config.getInt("ui.table.max-rows", 2000);
                    if (model.getLogData().size() > maxRows) {
                        Platform.runLater(() -> model.getLogData().remove(0, model.getLogData().size() - maxRows));
                    }
                    if (model.isAutoFollowLatest()) scrollToBottom();
                }
            }
        });
    }

    private void scrollToBottom() {
        if (!view.getTable().getItems().isEmpty()) {
            Platform.runLater(() -> view.getTable().scrollTo(view.getTable().getItems().size() - 1));
        }
    }
}