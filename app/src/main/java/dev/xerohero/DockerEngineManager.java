package dev.xerohero;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Controls and monitors the underlying Docker container ecosystem.
 * Tracks lifecycle states and exposes reactive state properties to the UI.
 */
@Singleton
public class DockerEngineManager {

    private final AppConfig config;
    private final SimpleBooleanProperty runningProperty = new SimpleBooleanProperty(true);

    @Inject
    public DockerEngineManager(AppConfig config) {
        this.config = config;
        initializeDockerContext();
    }

    private void initializeDockerContext() {
        String dockerHost = config.getString("docker.host-endpoint", "unix:///var/run/docker.sock");
        System.out.println("🐳 [DOCKER ENGINE] Initializing socket proxy connection to: " + dockerHost);
    }

    /**
     * Exposes a read-only property so the Presenter can watch engine state safely.
     */
    public ReadOnlyBooleanProperty runningProperty() {
        return runningProperty;
    }

    public boolean isRunning() {
        return runningProperty.get();
    }

    public void restartContainer(String containerId) {
        System.out.println("🔄 [DOCKER ENGINE] Dispatching restart sequence to: " + containerId);
        runningProperty.set(true);
    }

    public void stopContainer(String containerId) {
        System.out.println("🛑 [DOCKER ENGINE] Gracefully shutting down execution thread context: " + containerId);
        runningProperty.set(false);
    }
}