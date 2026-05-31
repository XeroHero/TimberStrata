package dev.xerohero;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static javafx.collections.FXCollections.observableArrayList;

public class DashboardApp extends Application {

    private final ObservableList<LogEntry> logData = observableArrayList();
    private Label engineStatusLabel;

    // --- Metrics & Counters ---
    private Label errorCountLabel;
    private int errorCount = 0;

    private Label warnCountLabel;
    private int warnCount = 0;
    private VBox warnCard;

    private Label debugCountLabel;
    private int debugCount = 0;
    private VBox debugCard;

    // --- Tracking States for Dynamic Switching ---
    private volatile Path activeLogFilePath = null;
    private final AtomicLong lastKnownSize = new AtomicLong(0);
    private Label activeFileLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🌲 TimberStrata Dashboard");

        // --- Generate Programmatic Vector Icon ---
        try {
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(128, 128);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            gc.fillRoundRect(8, 8, 112, 112, 24, 24);

            gc.setStroke(javafx.scene.paint.Color.web("#34495e"));
            gc.setLineWidth(3);
            gc.strokeRoundRect(8, 8, 112, 112, 24, 24);

            gc.setStroke(javafx.scene.paint.Color.web("#2ecc71"));
            gc.setLineWidth(8);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

            gc.beginPath();
            gc.moveTo(35, 40);
            gc.lineTo(60, 55);
            gc.lineTo(35, 70);
            gc.stroke();

            gc.fillRect(70, 64, 25, 8);

            gc.setFill(javafx.scene.paint.Color.web("#3498db"));
            gc.fillRect(35, 88, 60, 6);

            gc.setFill(javafx.scene.paint.Color.web("#e67e22"));
            gc.fillRect(35, 98, 35, 6);

            javafx.scene.image.WritableImage appIcon = new javafx.scene.image.WritableImage(128, 128);
            canvas.snapshot(null, appIcon);
            primaryStage.getIcons().add(appIcon);

        } catch (Exception e) {
            System.err.println("Failed to render vector icon: " + e.getMessage());
        }

        // --- Top Bar: Control Panel ---
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("TimberStrata Engine Control");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button startDockerBtn = new Button("▶ Start Docker Engine");
        Button stopDockerBtn = new Button("■ Stop Engine");
        engineStatusLabel = new Label("Status: Checking...");
        engineStatusLabel.setStyle("-fx-text-fill: #bdc3c7;");

        Button chooseFileBtn = new Button("📁 Stream File...");
        chooseFileBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        activeFileLabel = new Label("No log file selected (Engine Paused)");
        activeFileLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-style: italic;");

        topBar.getChildren().addAll(titleLabel, startDockerBtn, stopDockerBtn, engineStatusLabel, chooseFileBtn, activeFileLabel);

        // --- Left Sidebar: Analytics Counter ---
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #ecf0f1;");

        Label statsTitle = new Label("Metrics Dashboard");
        statsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        sidebar.getChildren().add(statsTitle);

        // 1. Error Card: Always Visible
        VBox errorCard = new VBox(5);
        errorCard.setPadding(new Insets(10));
        errorCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
        errorCountLabel = new Label("🚨 Errors: 0");
        errorCountLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 13px; -fx-font-weight: bold;");
        errorCard.getChildren().add(errorCountLabel);
        sidebar.getChildren().add(errorCard);

        // 2. Warning Card: Conditional Visibility
        warnCard = new VBox(5);
        warnCard.setPadding(new Insets(10));
        warnCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
        warnCountLabel = new Label("⚠️ Warnings: 0");
        warnCountLabel.setStyle("-fx-text-fill: #d35400; -fx-font-size: 13px; -fx-font-weight: bold;");
        warnCard.getChildren().add(warnCountLabel);

        warnCard.setVisible(false);
        warnCard.managedProperty().bind(warnCard.visibleProperty());
        sidebar.getChildren().add(warnCard);

        // 3. Debug Card: Conditional Visibility
        debugCard = new VBox(5);
        debugCard.setPadding(new Insets(10));
        debugCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
        debugCountLabel = new Label("⚙️ Debug Lines: 0");
        debugCountLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 13px; -fx-font-weight: bold;");
        debugCard.getChildren().add(debugCountLabel);

        debugCard.setVisible(false);
        debugCard.managedProperty().bind(debugCard.visibleProperty());
        sidebar.getChildren().add(debugCard);

        // --- Center Panel: Real-time Data Table ---
        TableView<LogEntry> table = new TableView<>();
        table.setItems(logData);

        TableColumn<LogEntry, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(data -> data.getValue().timestampProperty());
        colTime.setPrefWidth(150);

        TableColumn<LogEntry, String> colLevel = new TableColumn<>("Level");
        colLevel.setCellValueFactory(data -> data.getValue().levelProperty());
        colLevel.setPrefWidth(80);

        TableColumn<LogEntry, String> colLogger = new TableColumn<>("Logger");
        colLogger.setCellValueFactory(data -> data.getValue().loggerProperty());
        colLogger.setPrefWidth(120);

        TableColumn<LogEntry, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(data -> data.getValue().messageProperty());
        colMsg.setPrefWidth(350);

        table.getColumns().addAll(colTime, colLevel, colLogger, colMsg);

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("ERROR".equals(item.levelProperty().get())) {
                    setStyle("-fx-background-color: #fce4d6;");
                } else if ("WARN".equals(item.levelProperty().get())) {
                    setStyle("-fx-background-color: #fff2cc;");
                } else {
                    setStyle("");
                }
            }
        });

        // --- Layout Assembly ---
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(table);

        // --- Wire up Button Behaviors ---
        startDockerBtn.setOnAction(e -> executeSystemCommand("docker start timberstrata"));
        stopDockerBtn.setOnAction(e -> executeSystemCommand("docker stop timberstrata"));

        // Wire up the Native macOS Finder Picker
        chooseFileBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Open Log Source File");

            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Log Files (*.log, *.txt)", "*.log", "*.txt"),
                    new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );

            java.io.File initialDir = new java.io.File("logs");
            if (initialDir.exists()) {
                fileChooser.setInitialDirectory(initialDir);
            }

            java.io.File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                Platform.runLater(() -> {
                    logData.clear();
                    errorCount = 0;
                    errorCountLabel.setText("🚨 Errors: 0");

                    warnCount = 0;
                    warnCountLabel.setText("⚠️ Warnings: 0");
                    warnCard.setVisible(false);

                    debugCount = 0;
                    debugCountLabel.setText("⚙️ Debug Lines: 0");
                    debugCard.setVisible(false);

                    activeFileLabel.setText("Streaming: " + selectedFile.getName());
                    activeFileLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                });

                lastKnownSize.set(0);
                activeLogFilePath = selectedFile.toPath();
            }
        });

        startDirectorySyncLoop();

        Scene scene = new Scene(root, 950, 550);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startDirectorySyncLoop() {
        Thread syncThread = new Thread(() -> {
            while (true) {
                try {
                    checkDockerContainerStatus();

                    java.nio.file.Path currentFile = activeLogFilePath;

                    if (currentFile != null && Files.exists(currentFile)) {
                        long currentSize = Files.size(currentFile);
                        long knownSize = lastKnownSize.get();

                        if (currentSize > knownSize) {
                            parseNewLines(currentFile, knownSize);
                            lastKnownSize.set(currentSize);
                        } else if (currentSize < knownSize) {
                            Platform.runLater(() -> {
                                logData.clear();
                                errorCount = 0;
                                errorCountLabel.setText("🚨 Errors: 0");
                                warnCount = 0;
                                warnCountLabel.setText("⚠️ Warnings: 0");
                                warnCard.setVisible(false);
                                debugCount = 0;
                                debugCountLabel.setText("⚙️ Debug Lines: 0");
                                debugCard.setVisible(false);
                            });
                            lastKnownSize.set(0);
                        }
                    }
                    Thread.sleep(1500);
                } catch (Exception e) {
                    System.err.println("Polling core error: " + e.getMessage());
                }
            }
        });
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void parseNewLines(Path path, long skipBytes) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            reader.skip(skipBytes);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Map<String, String> parsed = LogParser.parseLine(line);
                if (parsed != null) {
                    LogEntry entry = new LogEntry(
                            parsed.get("timestamp"),
                            parsed.get("level"),
                            parsed.get("logger"),
                            parsed.get("message")
                    );
                    Platform.runLater(() -> {
                        logData.add(0, entry);
                        String severity = entry.levelProperty().get();
                        if ("ERROR".equals(severity)) {
                            errorCount++;
                            errorCountLabel.setText("🚨 Errors: " + errorCount);
                        } else if ("WARN".equals(severity)) {
                            warnCount++;
                            warnCountLabel.setText("⚠️ Warnings: " + warnCount);
                            if (warnCount > 0) {
                                warnCard.setVisible(true);
                            }
                        } else if ("DEBUG".equals(severity)) {
                            debugCount++;
                            debugCountLabel.setText("⚙️ Debug Lines: " + debugCount);
                            if (debugCount > 0) {
                                debugCard.setVisible(true);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("GUI sync reader error: " + e.getMessage());
        }
    }

    private void checkDockerContainerStatus() {
        boolean isRunning = false;
        try {
            Process process = Runtime.getRuntime().exec("docker inspect -f {{.State.Running}} timberstrata");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String out = r.readLine();
                if (out != null && out.trim().equals("true")) {
                    isRunning = true;
                }
            }
        } catch (Exception ignored) {}

        final boolean runningState = isRunning;
        Platform.runLater(() -> {
            if (runningState) {
                engineStatusLabel.setText("Status: Container Active (CLI Engine Running)");
                engineStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            } else {
                engineStatusLabel.setText("Status: Container Offline");
                engineStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
    }

    private void executeSystemCommand(String command) {
        try {
            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            System.err.println("Failed to execute action: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}