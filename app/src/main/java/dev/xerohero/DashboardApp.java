package dev.xerohero;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class DashboardApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("🚀 [BOOTSTRAP] Initializing dependency injection graph context...");

        try {
            Injector injector = Guice.createInjector(new AppModule());

            System.out.println("📦 [BOOTSTRAP] Resolving fortified DashboardView container instance...");
            DashboardView dashboardView = injector.getInstance(DashboardView.class);

            System.out.println("🔧 [BOOTSTRAP] Passing Stage context down to view layout initializer...");
            dashboardView.initializeView(primaryStage);

            // --- SINGLE ATOMIC LIFECYCLE HOOK TRIGGER ---
            System.out.println("⚙️ [BOOTSTRAP] Launching Log Engine structural daemon pipeline...");
            LogDirectoryWatcher watcher = injector.getInstance(LogDirectoryWatcher.class);

            // Set up test data file target configuration out of the gate
            watcher.changeWatchedDirectory(new File("/Users/lorenzobattilocchi/Git/TimberStrata/logs"));
            watcher.startLoop();

            System.out.println("🖼️ [BOOTSTRAP] Mounting scene framing container wrapper...");
            Scene scene = new Scene(dashboardView, 1100, 750);
            primaryStage.setScene(scene);
            primaryStage.setTitle("🌲 TimberStrata Analysis Station");

            System.out.println("🖥️ [BOOTSTRAP] Displaying application primary stage context.");
            primaryStage.show();

        } catch (Exception ex) {
            System.err.println("💥 [CRITICAL APP CRASH] Dependency Injection or View instantiation pipeline failed:");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}