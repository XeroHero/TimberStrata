package dev.xerohero;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application launcher for the TimberStrata Engine.
 * Orchestrates the Guice DI initialization pipeline and mounts the MVP UI layer.
 */
public class MainApp extends Application {

    @Inject
    private DashboardView dashboardView;

    @Inject
    private DashboardPresenter dashboardPresenter;

    @Override
    public void init() throws Exception {
        // Initialize the Guice object container graph and satisfy the @Inject fields above
        Guice.createInjector(new TimberStrataModule()).injectMembers(this);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Let the presenter hook up sorted lists, components, actions, and animation loops
            dashboardPresenter.bindView(primaryStage);

            // Render the root view graph structure onto the layout stage
            Scene scene = new Scene(dashboardView, 1024, 768);

            primaryStage.setScene(scene);
            primaryStage.setTitle("TimberStrata Core Management Console");
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("💥 [FATAL APPLICATION BOOT ERROR] Failed to display core layout console:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Kick off the JavaFX native thread application wrapper lifecycle
        launch(args);
    }
}