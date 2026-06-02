package dev.xerohero;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardApp extends Application {

    private Injector injector;
    private LogDirectoryWatcher watcher;
    private DockerEngineManager dockerManager;
    private Timeline dockerHeartbeat;

    @Override
    public void init() {
        // 1. Initialize the Guice object container context before the UI layer spins up
        injector = Guice.createInjector(new TimberStrataModule());

        // 2. Extract references needed directly by our lifecycle controls
        watcher = injector.getInstance(LogDirectoryWatcher.class);
        dockerManager = injector.getInstance(DockerEngineManager.class);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🌲 TimberStrata Dashboard");

        // 3. Ask Guice to instantiate the view. It will automatically build and
        // inject the list, registry, watcher, and docker manager behind the scenes!
        DashboardView view = injector.getInstance(DashboardView.class);
        view.initializeView(primaryStage);

        primaryStage.setScene(new Scene(view, 950, 550));
        primaryStage.show();

        // 4. Docker Heartbeat (Safe UI Timeline)
        dockerHeartbeat = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            boolean running = dockerManager.isContainerRunning();
            view.getEngineStatusLabel().setText(running ? "Status: Active" : "Status: Offline");
            view.getEngineStatusLabel().setStyle("-fx-text-fill: " + (running ? "#2ecc71" : "#e74c3c") + "; -fx-font-weight: bold;");
        }));
        dockerHeartbeat.setCycleCount(Timeline.INDEFINITE);
        dockerHeartbeat.play();

        // 5. Fire up the background filesystem streaming thread
        watcher.startLoop();

        // 6. Hardened Lifecycle Teardown
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