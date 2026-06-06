package dev.xerohero;

import com.google.inject.Inject;
import dev.xerohero.ai.AiAnalysisService;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.Random;

import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;

public class DashboardView extends BorderPane {

    private final ObservableList<LogEntry> logData;
    private final MetricRegistry metrics;
    private final LogDirectoryWatcher watcher;
    private final DockerEngineManager dockerManager;
    private final AiAnalysisService aiService;
    private final AppConfig config;

    private FilteredList<LogEntry> filteredLogData;
    private Label engineStatusLabel;
    private Label errorCountLabel;
    private Label activeFileLabel;
    private VBox sidebarCardContainer;

    // Auto-Scroll States
    private boolean autoFollowLatest = true;
    private Button toggleScrollBtn;

    // 🤖 Funny Claude-style thinking phrases
    private final String[] thinkingPhrases = {
            "Analyzing architectural life choices...",
            "Inverting binary trees in memory...",
            "Reticulating telemetry splines...",
            "Blaming the network configuration...",
            "Consulting stack overflow archives...",
            "Deeply contemplating garbage collection overhead...",
            "Aggressively parsing kernel panic states...",
            "Applying microservice psychological counseling...",
            "Re-indexing local container alignment space...",
            "Loggering the logger's logs...",
            "Chilling the brains..."
    };

    @Inject
    public DashboardView(ObservableList<LogEntry> logData, MetricRegistry metrics,
                         LogDirectoryWatcher watcher, DockerEngineManager dockerManager,
                         AiAnalysisService aiService, AppConfig config) {
        this.logData = logData;
        this.metrics = metrics;
        this.watcher = watcher;
        this.dockerManager = dockerManager;
        this.aiService = aiService;
        this.config = config;
    }

    private void openInspectorPopup(LogEntry selected) {
        if (selected == null) return;

        if (autoFollowLatest) {
            autoFollowLatest = false;
            Platform.runLater(() -> {
                toggleScrollBtn.setText("🛑 Auto-Follow: OFF");
                toggleScrollBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
            });
        }

        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Log Entry Inspector");
                alert.setHeaderText("Detailed Unabridged Log Message View:");

                String msgContent = selected.messageProperty().get();
                if (msgContent == null) msgContent = "No log details available.";

                TextArea textArea = new TextArea(msgContent);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefWidth(650);
                textArea.setPrefHeight(350);
                textArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

                VBox contentWrapper = new VBox(textArea);
                VBox.setVgrow(textArea, ALWAYS);
                contentWrapper.setPadding(new Insets(5));

                alert.getDialogPane().setContent(contentWrapper);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void initializeView(Stage stage) {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("TimberStrata Engine");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Button startBtn = new Button("▶ Start Engine");
        Button stopBtn = new Button("■ Stop Engine");

        engineStatusLabel = new Label("Status: Active");
        engineStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        activeFileLabel = new Label("Monitoring: test-logs");
        activeFileLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-style: italic;");
        Button chooseFileBtn = new Button("📁 Watch Folder...");

        toggleScrollBtn = new Button("🔄 Auto-Follow: ON");
        toggleScrollBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        topBar.getChildren().addAll(titleLabel, startBtn, stopBtn, engineStatusLabel, chooseFileBtn, toggleScrollBtn, activeFileLabel);

        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #ecf0f1;");

        sidebarCardContainer = new VBox(10);
        VBox errorCard = new VBox(5);
        errorCard.setPadding(new Insets(10));
        errorCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
        errorCountLabel = new Label("🚨 Errors: 0");
        errorCountLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
        errorCard.getChildren().add(errorCountLabel);
        sidebarCardContainer.getChildren().add(errorCard);

        metrics.errorCountProperty().addListener((obs, old, newVal) ->
                Platform.runLater(() -> errorCountLabel.setText("🚨 Errors: " + newVal))
        );

        VBox customTagBox = new VBox(8);
        TextField customTagField = new TextField();
        customTagField.setPromptText("e.g., FATAL, WARN");
        Button addTagBtn = new Button("➕ Add Metric Card");
        addTagBtn.setMaxWidth(Double.MAX_VALUE);

        metrics.getCustomCounters().addListener((MapChangeListener<String, javafx.beans.property.IntegerProperty>) change -> {
            if (change.wasAdded()) {
                String tag = change.getKey();
                javafx.beans.property.IntegerProperty valueProp = change.getValueAdded();

                Platform.runLater(() -> {
                    VBox newCard = new VBox(5);
                    newCard.setPadding(new Insets(10));
                    newCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
                    Label newLabel = new Label("🏷️ " + tag + ": 0");
                    newLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                    newCard.getChildren().add(newLabel);

                    valueProp.addListener((obs, old, nv) -> Platform.runLater(() -> newLabel.setText("🏷️ " + tag + ": " + nv)));
                    sidebarCardContainer.getChildren().add(newCard);
                    customTagField.clear();
                });
            }
        });
        addTagBtn.setOnAction(e -> metrics.registerTag(customTagField.getText()));

        customTagBox.getChildren().addAll(new Label("Track Custom Tag:"), customTagField, addTagBtn);

        VBox quickInspectBox = new VBox(5);
        Button manualInspectBtn = new Button("🔍 Inspect Selected Row");
        manualInspectBtn.setMaxWidth(Double.MAX_VALUE);
        manualInspectBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        Label backupHint = new Label("Shortcut: Select line + Press SPACE");
        backupHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        quickInspectBox.getChildren().addAll(manualInspectBtn, backupHint);

        sidebar.getChildren().addAll(sidebarCardContainer, new Separator(), customTagBox, new Separator(), quickInspectBox);

        filteredLogData = new FilteredList<>(this.logData, p -> true);
        TableView<LogEntry> table = new TableView<>(filteredLogData);
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<LogEntry, Boolean> colMarked = new TableColumn<>("📌");
        colMarked.setCellValueFactory(d -> d.getValue().markedProperty());
        colMarked.setCellFactory(c -> {
            CheckBoxTableCell<LogEntry, Boolean> cell = new CheckBoxTableCell<>();
            cell.setEditable(true);
            return cell;
        });
        colMarked.setEditable(true);

        TableColumn<LogEntry, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(d -> d.getValue().timestampProperty());

        TableColumn<LogEntry, String> colService = new TableColumn<>("Level");
        colService.setCellValueFactory(d -> d.getValue().levelProperty());

        TableColumn<LogEntry, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(d -> d.getValue().messageProperty());
        colMsg.setPrefWidth(550);
        colMsg.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item.contains("\n")) {
                        setText(item.split("\n")[0] + "  [🔍 Multiline]");
                    } else {
                        setText(item);
                    }
                }
            }
        });

        table.getColumns().addAll(colMarked, colTime, colService, colMsg);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Filter visible lines...");
        searchField.textProperty().addListener((obs, old, nv) -> filteredLogData.setPredicate(entry -> {
            if (nv == null || nv.trim().isEmpty()) return true;
            String f = nv.toLowerCase().trim();
            return entry.messageProperty().get().toLowerCase().contains(f) || entry.levelProperty().get().toLowerCase().contains(f);
        }));

        table.setRowFactory(tv -> {
            TableRow<LogEntry> row = new TableRow<>(){
                private final Tooltip stackTraceTooltip = new Tooltip();

                @Override
                protected void updateItem(LogEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    setTooltip(null);
                    if (empty || item == null) {
                        setStyle("");
                        return;
                    }

                    if (item.isMarked()) setStyle("-fx-background-color: #e8daef; -fx-font-weight: bold;");
                    else if ("ERROR".equals(item.levelProperty().get())) setStyle("-fx-background-color: #fce4d6;");
                    else if ("WARN".equals(item.levelProperty().get())) setStyle("-fx-background-color: #fef9e7;");
                    else setStyle("");

                    String fullMessage = item.messageProperty().get();
                    if (fullMessage != null && fullMessage.contains("\n")) {
                        stackTraceTooltip.setText(fullMessage);
                        stackTraceTooltip.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: #ecf0f1;");
                        setTooltip(stackTraceTooltip);
                    }
                }
            };

            ContextMenu rowMenu = new ContextMenu();
            MenuItem explainItem = new MenuItem("🤖 Explain with TimberAI");
            explainItem.setOnAction(event -> {
                LogEntry selected = row.getItem();
                if (selected != null) {
                    String logString = String.format("[%s] [%s] %s",
                            selected.timestampProperty().get(),
                            selected.levelProperty().get(),
                            selected.messageProperty().get()
                    );

                    System.out.println("🧠 [AI TRIGGER] Dispatching payload to asynchronous analysis thread...");

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("TimberAI Diagnostic Report");
                    alert.setHeaderText("Thinking...");

                    ProgressIndicator progressIndicator = new ProgressIndicator(-1.0);
                    progressIndicator.setPrefSize(50, 50);

                    // Initial thinking tag label initialization
                    Label loadingLabel = new Label("Initializing local model context layer...");
                    loadingLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

                    VBox loadingBox = new VBox(15, progressIndicator, loadingLabel);
                    loadingBox.setAlignment(Pos.CENTER);
                    loadingBox.setPadding(new Insets(30));
                    loadingBox.setPrefWidth(550);
                    loadingBox.setPrefHeight(350);

                    alert.getDialogPane().setContent(loadingBox);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

                    // 🛠️ CLAUDE-STYLE REFRESH TIMELINE LOOP ANIMATION
                    Random rand = new Random();
                    Timeline thinkingTimeline = new Timeline(new KeyFrame(Duration.seconds(1.2), e -> {
                        String freshPhrase = thinkingPhrases[rand.nextInt(thinkingPhrases.length)];
                        loadingLabel.setText(freshPhrase);
                    }));
                    thinkingTimeline.setCycleCount(Animation.INDEFINITE);
                    thinkingTimeline.play();

                    aiService.explainLogAsync(logString)
                            .thenAccept(explanation -> {
                                System.out.println("✅ [AI RESPONSE RECEIVED] Payload back from worker thread. Updating UI context...");
                                Platform.runLater(() -> {
                                    // 🛠️ Stop the animation cycle completely to avoid memory resource leaks
                                    thinkingTimeline.stop();

                                    alert.setHeaderText("Log Context Analysis Summary");

                                    TextArea textArea = new TextArea(explanation);
                                    textArea.setEditable(false);
                                    textArea.setWrapText(true);
                                    textArea.setPrefWidth(550);
                                    textArea.setPrefHeight(350);
                                    textArea.setStyle("-fx-font-family: 'Helvetica Neue', Arial; -fx-font-size: 13px;");

                                    alert.getDialogPane().setContent(textArea);
                                });
                            })
                            .exceptionally(ex -> {
                                Platform.runLater(() -> {
                                    thinkingTimeline.stop();
                                    System.err.println("💥 [CRITICAL BACKGROUND THREAD CRASH] The AI future pipeline failed:");
                                    alert.setHeaderText("Analysis Pipeline Failure");
                                    Label failureLabel = new Label("❌ Local AI processing failed or timed out.");
                                    failureLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                                    alert.getDialogPane().setContent(new VBox(failureLabel));
                                    if (ex != null) ex.printStackTrace();
                                });
                                return null;
                            });

                    alert.showAndWait();
                }
            });
            rowMenu.getItems().add(explainItem);

            row.itemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    row.setContextMenu(rowMenu);
                } else {
                    row.setContextMenu(null);
                }
            });

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    LogEntry targetItem = row.getItem();
                    if (targetItem != null) {
                        openInspectorPopup(targetItem);
                        event.consume();
                    }
                }
            });

            return row;
        });

        manualInspectBtn.setOnAction(e -> {
            LogEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) openInspectorPopup(selected);
        });

        table.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == javafx.scene.input.KeyCode.SPACE) {
                LogEntry selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openInspectorPopup(selected);
                    keyEvent.consume();
                }
            }
        });

        this.logData.addListener((javafx.collections.ListChangeListener<LogEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    int maxRows = config.getInt("ui.table.max-rows", 2000);
                    if (this.logData.size() > maxRows) {
                        Platform.runLater(() -> this.logData.remove(0, this.logData.size() - maxRows));
                    }
                    if (autoFollowLatest) {
                        Platform.runLater(() -> table.scrollTo(table.getItems().size() - 1));
                    }
                }
            }
        });

        toggleScrollBtn.setOnAction(e -> {
            autoFollowLatest = !autoFollowLatest;
            if (autoFollowLatest) {
                toggleScrollBtn.setText("🔄 Auto-Follow: ON");
                toggleScrollBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                if (!table.getItems().isEmpty()) {
                    table.scrollTo(table.getItems().size() - 1);
                }
            } else {
                toggleScrollBtn.setText("🛑 Auto-Follow: OFF");
                toggleScrollBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        });

        VBox centerLayout = new VBox(10, searchField, table);
        VBox.setVgrow(table, ALWAYS);
        centerLayout.setPadding(new Insets(10));

        this.setTop(topBar);
        this.setLeft(sidebar);
        this.setCenter(centerLayout);

        chooseFileBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                this.logData.clear();
                metrics.resetAll();
                activeFileLabel.setText("Monitoring: " + selected.getName());
                watcher.changeWatchedDirectory(selected);
            }
        });
    }

    public Label getEngineStatusLabel() { return engineStatusLabel; }
}