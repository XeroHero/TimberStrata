package dev.xerohero;

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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;

public class DashboardView extends BorderPane {

    private final ObservableList<LogEntry> logData;
    private final MetricRegistry metrics;
    private final LogDirectoryWatcher watcher;
    private final DockerEngineManager dockerManager;

    private FilteredList<LogEntry> filteredLogData;
    private Label engineStatusLabel;
    private Label errorCountLabel;
    private Label activeFileLabel;
    private VBox sidebarCardContainer;

    public DashboardView(ObservableList<LogEntry> logData, MetricRegistry metrics,
                         LogDirectoryWatcher watcher, DockerEngineManager dockerManager) {
        this.logData = logData;
        this.metrics = metrics;
        this.watcher = watcher;
        this.dockerManager = dockerManager;
    }

    /**
     * Constructs and arranges the visual component tree hierarchy.
     */
    public void initializeView(Stage stage) {
        // --- Generate Programmatic Vector Icon ---
        try {
            Canvas canvas = new Canvas(128, 128);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.web("#2c3e50"));
            gc.fillRoundRect(8, 8, 112, 112, 24, 24);

            gc.setStroke(Color.web("#34495e"));
            gc.setLineWidth(3);
            gc.strokeRoundRect(8, 8, 112, 112, 24, 24);

            gc.setStroke(Color.web("#2ecc71"));
            gc.setLineWidth(8);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);

            gc.beginPath();
            gc.moveTo(35, 40);
            gc.lineTo(60, 55);
            gc.lineTo(35, 70);
            gc.stroke();

            gc.fillRect(70, 64, 25, 8);

            gc.setFill(Color.web("#3498db"));
            gc.fillRect(35, 88, 60, 6);

            gc.setFill(Color.web("#e67e22"));
            gc.fillRect(35, 98, 35, 6);

            WritableImage appIcon = new WritableImage(128, 128);
            canvas.snapshot(null, appIcon);
            stage.getIcons().add(appIcon);

        } catch (Exception e) {
            System.err.println("Failed to render vector icon: " + e.getMessage());
        }

        // --- Top Control Ribbon ---
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

        topBar.getChildren().addAll(titleLabel, startBtn, stopBtn, engineStatusLabel, chooseFileBtn, activeFileLabel);

        // --- Sidebar Analytics Panel ---
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

        // Bind standard error metrics directly to model modifications reactively
        metrics.errorCountProperty().addListener((obs, old, newVal) ->
                errorCountLabel.setText("🚨 Errors: " + newVal)
        );

        // Dynamic Tag Input Field Setup
        VBox customTagBox = new VBox(8);
        TextField customTagField = new TextField();
        customTagField.setPromptText("e.g., FATAL, WARN, INFO");
        Button addTagBtn = new Button("➕ Add Metric Card");
        addTagBtn.setMaxWidth(Double.MAX_VALUE);
        addTagBtn.setOnAction(e -> metrics.registerTag(customTagField.getText()));

        // Explicitly typed listener tracking map mutations cleanly to satisfy the compiler
        MapChangeListener<String, javafx.beans.property.IntegerProperty> counterListener = change -> {
            if (change.wasAdded()) {
                String tag = change.getKey();
                javafx.beans.property.IntegerProperty valueProp = change.getValueAdded();

                VBox newCard = new VBox(5);
                newCard.setPadding(new Insets(10));
                newCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
                Label newLabel = new Label("🏷️ " + tag + ": 0");
                newLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                newCard.getChildren().add(newLabel);

                valueProp.addListener((obs, old, newVal) ->
                        Platform.runLater(() -> newLabel.setText("🏷️ " + tag + ": " + newVal))
                );

                Platform.runLater(() -> {
                    sidebarCardContainer.getChildren().add(newCard);
                    customTagField.clear();
                });
            }
        };

        metrics.getCustomCounters().addListener(counterListener);

        customTagBox.getChildren().addAll(new Label("Track Custom Tag:"), customTagField, addTagBtn);
        sidebar.getChildren().addAll(sidebarCardContainer, new Separator(), customTagBox);

        // --- Center Panel Table ---
        filteredLogData = new FilteredList<>(logData, p -> true);
        TableView<LogEntry> table = new TableView<>(filteredLogData);
        table.setEditable(true);

        TableColumn<LogEntry, Boolean> colMarked = new TableColumn<>("📌");
        colMarked.setCellValueFactory(d -> d.getValue().markedProperty());
        colMarked.setCellFactory(c -> new CheckBoxTableCell<>());
        colMarked.setEditable(true);

        TableColumn<LogEntry, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(d -> d.getValue().timestampProperty());

        TableColumn<LogEntry, String> colLevel = new TableColumn<>("Level");
        colLevel.setCellValueFactory(d -> d.getValue().levelProperty());

        TableColumn<LogEntry, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(d -> d.getValue().messageProperty());
        colMsg.setPrefWidth(350);

        table.getColumns().addAll(colMarked, colTime, colLevel, colMsg);

        // Setup filter predicate text processing loops
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Quick filter text...");
        searchField.textProperty().addListener((obs, old, nv) -> filteredLogData.setPredicate(entry -> {
            if (nv == null || nv.trim().isEmpty()) return true;
            String f = nv.toLowerCase().trim();
            if (":marked".equals(f) || ":pinned".equals(f)) return entry.isMarked();
            return entry.messageProperty().get().toLowerCase().contains(f) ||
                    entry.levelProperty().get().toLowerCase().contains(f) ||
                    entry.loggerProperty().get().toLowerCase().contains(f);
        }));

        // Dynamic row color bindings with row-recycling empty checks fixed
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isMarked()) {
                    setStyle("-fx-background-color: #e8daef; -fx-font-weight: bold;");
                } else if ("ERROR".equals(item.levelProperty().get())) {
                    setStyle("-fx-background-color: #fce4d6;");
                } else if ("WARN".equals(item.levelProperty().get())) {
                    setStyle("-fx-background-color: #fff2cc;");
                } else {
                    setStyle("");
                }
            }
        });

        VBox centerLayout = new VBox(10, searchField, table);
        VBox.setVgrow(table, ALWAYS);
        centerLayout.setPadding(new Insets(10));

        // Assemble content sub-nodes onto our root layout container
        this.setTop(topBar);
        this.setLeft(sidebar);
        this.setCenter(centerLayout);

        // --- Action Configurations connected to decoupled services ---
        startBtn.setOnAction(e -> dockerManager.executeCommand("docker start timberstrata"));
        stopBtn.setOnAction(e -> dockerManager.executeCommand("docker stop timberstrata"));

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
    }

    // Expose structural component tracking handlers safely for background loops to tap into
    public Label getEngineStatusLabel() {
        return engineStatusLabel;
    }
}