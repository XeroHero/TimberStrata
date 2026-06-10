package dev.xerohero;

import com.google.inject.Guice;
import com.google.inject.Inject;
import dev.xerohero.ui.DashboardPresenter;
import dev.xerohero.ui.DashboardView;
import dev.xerohero.ui.DashboardModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardApp extends Application {

    @Inject
    private DashboardView dashboardView;

    @Inject
    private DashboardPresenter dashboardPresenter;

    @Override
    public void init() throws Exception {
        // Bootstraps the Guice DI container and injects fields cleanly
        Guice.createInjector(new TimberStrataModule()).injectMembers(this);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Let the presenter wire up layout structures, filters, and background AI engines
            dashboardPresenter.bindView(primaryStage);

            // 🛑 FIXING ERROR (36, 37): JavaFX Scene expects (Parent), or (Parent, width, height)
            // Passing integers directly without floating decimals can confuse overloaded constructors.
            Scene scene = new Scene(dashboardView, 1024.0, 768.0);

            primaryStage.setScene(scene);
            primaryStage.setTitle("TimberStrata Core Management Console");
            primaryStage.setMinWidth(900.0);
            primaryStage.setMinHeight(600.0);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("💥 [FATAL APPLICATION BOOT ERROR] Failed to display console layout:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}