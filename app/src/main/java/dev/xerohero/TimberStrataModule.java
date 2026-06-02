package dev.xerohero;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TimberStrataModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bind singletons cleanly
        bind(AppConfig.class).in(Singleton.class);
        bind(MetricRegistry.class).in(Singleton.class);
        bind(LogDirectoryWatcher.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public ObservableList<LogEntry> provideLogData() {
        return FXCollections.observableArrayList();
    }

    @Provides
    @Singleton
    public DockerEngineManager provideDockerEngineManager(AppConfig config) {
        // Read the target container dynamically from our configuration file!
        String containerName = config.getString("docker.container.name", "timberstrata");
        return new DockerEngineManager(containerName);
    }
}