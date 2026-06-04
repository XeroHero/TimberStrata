package dev.xerohero;

import com.google.inject.Inject;
import dev.xerohero.ai.AiAnalysisService;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;

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

        // Both of these are already on the FX thread from the mouse click
        autoFollowLatest = false;
        toggleScrollBtn.setText("🛑 Auto-Follow: OFF");
        toggleScrollBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

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
    }

    public void initializeView(Stage stage) {
        System.out.println("🏁 [INIT ENTRY] Starting main dashboard view layout construction branch...");

        // --- Dynamic Window Icon Generation ---
        try {
            Canvas canvas = new Canvas(128, 128);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.web("#2c3e50"));
            gc.fillRoundRect(8, 8, 112, 112, 24, 24);
            WritableImage appIcon = new WritableImage(128, 128);
            canvas.snapshot(null, appIcon);
            stage.getIcons().add(appIcon);
        } catch (Exception ignored) {}

        // --- Top Control Ribbon Header Layout ---
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("TimberStrata Engine");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Button startBtn = new Button("▶ Start Engine");
        Button stopBtn = new Button("■ Stop Engine");

        engineStatusLabel = new Label("Status: Checking...");
        engineStatusLabel.setStyle("-fx-text-fill: #bdc3c7;");

        activeFileLabel = new Label("No folder monitored");
        activeFileLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-style: italic;");
        Button chooseFileBtn = new Button("📁 Watch Folder...");

        toggleScrollBtn = new Button("🔄 Auto-Follow: ON");
        toggleScrollBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        topBar.getChildren().addAll(titleLabel, startBtn, stopBtn, engineStatusLabel, chooseFileBtn, toggleScrollBtn, activeFileLabel);

        // --- Sidebar Summary Panel ---
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

        metrics.errorCountProperty().addListener((obs, old, newVal) -> errorCountLabel.setText("🚨 Errors: " + newVal));

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

        // --- Center Stream Data Grid Layout ---
        filteredLogData = new FilteredList<>(logData, p -> true);
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
                    setText(item.contains("\n") ? item.split("\n")[0] + "  [🔍 Multiline]" : item);
                }
            }
        });

        // ✅ Row factory set BEFORE columns are added
        table.setRowFactory(tv -> {
            TableRow<LogEntry> row = new TableRow<>() {
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
                // ← no setOnMouseClicked here anymore
            };

            return row;
        });

        // ✅ Columns added AFTER row factory
        table.getColumns().addAll(colMarked, colTime, colService, colMsg);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Filter visible lines...");
        searchField.textProperty().addListener((obs, old, nv) -> filteredLogData.setPredicate(entry -> {
            if (nv == null || nv.trim().isEmpty()) return true;
            String f = nv.toLowerCase().trim();
            return entry.messageProperty().get().toLowerCase().contains(f) || entry.levelProperty().get().toLowerCase().contains(f);
        }));

        // --- Fallback & Keyboard Wire-Ups ---
        manualInspectBtn.setOnAction(e -> {
            LogEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) openInspectorPopup(selected);
        });
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TableRow<LogEntry> row = null;
                // Walk up the scene graph from the click target to find the TableRow
                javafx.scene.Node node = (javafx.scene.Node) event.getTarget();
                while (node != null && !(node instanceof TableRow)) {
                    node = node.getParent();
                }
                if (node instanceof TableRow) {
                    @SuppressWarnings("unchecked")
                    TableRow<LogEntry> typedRow = (TableRow<LogEntry>) node;
                    LogEntry item = typedRow.getItem();
                    if (item != null && !typedRow.isEmpty()) {
                        openInspectorPopup(item);
                        event.consume();
                    }
                }
            }
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

        // --- Context Menu AI Hook Integration ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem explainItem = new MenuItem("🤖 Explain with TimberAI");
        explainItem.setOnAction(event -> {
            LogEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String logString = String.format("[%s] [%s] %s", selected.timestampProperty().get(), selected.levelProperty().get(), selected.messageProperty().get());
                aiService.explainLogAsync(logString)
                        .thenAccept(explanation -> Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("TimberAI Diagnostic Report");
                            alert.setHeaderText("Log Context Analysis Summary");
                            alert.setContentText(explanation);
                            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                            alert.showAndWait();
                        }));
            }
        });
        contextMenu.getItems().add(explainItem);
        table.setContextMenu(contextMenu);

        // --- Auto-Scroll Listener Pipeline ---
        logData.addListener((javafx.collections.ListChangeListener<LogEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    int maxRows = config.getInt("ui.table.max-rows", 2000);
                    if (logData.size() > maxRows) {
                        Platform.runLater(() -> logData.remove(0, logData.size() - maxRows));
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

        startBtn.setOnAction(e -> dockerManager.executeCommand("docker start " + config.getString("docker.container.name", "timberstrata")));
        stopBtn.setOnAction(e -> dockerManager.executeCommand("docker stop " + config.getString("docker.container.name", "timberstrata")));

        chooseFileBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                logData.clear();
                metrics.resetAll();
                activeFileLabel.setText("Monitoring: " + selected.getName());
                watcher.changeWatchedDirectory(selected);
            }
        });

        System.out.println("✅ [INIT COMPLETE] Dashboard view layout execution sequence concluded successfully.");
    }

    public Label getEngineStatusLabel() { return engineStatusLabel; }
}