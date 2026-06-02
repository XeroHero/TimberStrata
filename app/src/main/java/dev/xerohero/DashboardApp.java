package dev.xerohero;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import static javafx.collections.FXCollections.observableArrayList;

public class DashboardApp extends Application {

    private final ObservableList<LogEntry> logData = observableArrayList();
    private final MetricRegistry metrics = new MetricRegistry();
    private final LogDirectoryWatcher watcher = new LogDirectoryWatcher(logData, metrics);
    private final DockerEngineManager dockerManager = new DockerEngineManager("timberstrata");
    private Timeline dockerHeartbeat;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🌲 TimberStrata Dashboard");

        // 1. Initialize the layout view passing down decoupled engine contexts
        DashboardView view = new DashboardView(logData, metrics, watcher, dockerManager);
        view.initializeView(primaryStage);

        primaryStage.setScene(new Scene(view, 950, 550));
        primaryStage.show();

        // 2. Optimized Docker Heartbeat: Runs safely on the JavaFX Application Thread
        dockerHeartbeat = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            boolean running = dockerManager.isContainerRunning();
            view.getEngineStatusLabel().setText(running ? "Status: Active" : "Status: Offline");
            view.getEngineStatusLabel().setStyle("-fx-text-fill: " + (running ? "#2ecc71" : "#e74c3c") + "; -fx-font-weight: bold;");
        }));
        dockerHeartbeat.setCycleCount(Timeline.INDEFINITE);
        dockerHeartbeat.play();

        // 3. Fire up the hardened background file IO streaming loop
        watcher.startLoop();

        // 4. Hardened Lifecycle Teardown: Cleanly stop all engine attachments on exit
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Stopping background streaming services...");
            if (dockerHeartbeat != null) {
                dockerHeartbeat.stop();
            }
            watcher.stopLoop();
        });
    }

    public static void main(String[] args) { launch(args); }
}