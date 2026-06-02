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
        // 1. Initialize Guice context container before the toolkit layer fires up
        injector = Guice.createInjector(new TimberStrataModule());

        // 2. Resolve singleton references required directly by lifecycle orchestration
        watcher = injector.getInstance(LogDirectoryWatcher.class);
        dockerManager = injector.getInstance(DockerEngineManager.class);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🌲 TimberStrata Dashboard");

        // 3. Let Guice handle full dependency tree generation for the view automatically
        DashboardView view = injector.getInstance(DashboardView.class);
        view.initializeView(primaryStage);

        primaryStage.setScene(new Scene(view, 950, 550));
        primaryStage.show();

        // 4. Safely query our externalized environment configurations
        AppConfig config = injector.getInstance(AppConfig.class);
        int heartbeatSeconds = config.getInt("docker.heartbeat.seconds", 2);

        // 5. Optimized Docker Heartbeat Timeline: Runs smoothly on the FX Application Thread
        dockerHeartbeat = new Timeline(new KeyFrame(Duration.seconds(heartbeatSeconds), event -> {
            boolean running = dockerManager.isContainerRunning();
            view.getEngineStatusLabel().setText(running ? "Status: Active" : "Status: Offline");
            view.getEngineStatusLabel().setStyle("-fx-text-fill: " + (running ? "#2ecc71" : "#e74c3c") + "; -fx-font-weight: bold;");
        }));
        dockerHeartbeat.setCycleCount(Timeline.INDEFINITE);
        dockerHeartbeat.play();

        // 6. Start background stream watcher
        watcher.startLoop();

        // 7. Graceful resource teardown on window closure
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