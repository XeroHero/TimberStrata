package dev.xerohero;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static javafx.collections.FXCollections.observableArrayList;

public class DashboardApp extends Application {

    private final ObservableList<LogEntry> logData = observableArrayList();
    private final MetricRegistry metrics = new MetricRegistry();
    private final LogDirectoryWatcher watcher = new LogDirectoryWatcher(logData, metrics);
    private final DockerEngineManager dockerManager = new DockerEngineManager("timberstrata");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🌲 TimberStrata Dashboard");

        DashboardView view = new DashboardView(logData, metrics, watcher, dockerManager);

        view.initializeView(primaryStage);

        primaryStage.setScene(new Scene(view, 950, 550));
        primaryStage.show();

        Thread statusThread = new Thread(() -> {
            while (true) {
                boolean running = dockerManager.isContainerRunning();

                Platform.runLater(() -> {
                    view.getEngineStatusLabel().setText(running ? "Status: Active" : "Status: Offline");
                    view.getEngineStatusLabel().setStyle("-fx-text-fill: " + (running ? "#2ecc71" : "#e74c3c") + "; -fx-font-weight: bold;");
                });

                try { Thread.sleep(2000); } catch (Exception ignored) {}
            }
        });
        statusThread.setDaemon(true);
        statusThread.start();

        watcher.startLoop();

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Stopping background streaming services...");
            watcher.stopLoop();
        });

    }

    public static void main(String[] args) { launch(args); }
}