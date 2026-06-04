package dev.xerohero;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.xerohero.ai.AiAnalysisService;
import dev.xerohero.core.DockerEngineManager;
import dev.xerohero.core.LogDirectoryWatcher;
import dev.xerohero.core.LogEntry;
import dev.xerohero.core.MetricRegistry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TimberStrataModule extends AbstractModule {

    @Override
    protected void configure() {
        // Core framework configurations bound cleanly in a single method
        bind(AppConfig.class).in(Singleton.class);
        bind(MetricRegistry.class).in(Singleton.class);
        bind(LogDirectoryWatcher.class).in(Singleton.class);
        bind(AiAnalysisService.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public ObservableList<LogEntry> provideLogData() {
        return FXCollections.observableArrayList();
    }

    @Provides
    @Singleton
    public DockerEngineManager provideDockerEngineManager(AppConfig config) {
        String containerName = config.getString("docker.container.name", "timberstrata");
        return new DockerEngineManager(containerName);
    }
}
