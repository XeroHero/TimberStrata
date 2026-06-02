package dev.xerohero;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TimberStrataModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bind core singletons that manage global application state
        bind(MetricRegistry.class).in(Singleton.class);
        bind(LogDirectoryWatcher.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public ObservableList<LogEntry> provideLogData() {
        // Guice will now inject this exact same list wherever requested
        return FXCollections.observableArrayList();
    }

    @Provides
    @Singleton
    public DockerEngineManager provideDockerEngineManager() {
        // Centralized configuration hook for the target container name
        return new DockerEngineManager("timberstrata");
    }
}