package dev.xerohero;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.xerohero.ai.AiAnalysisService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        // Concrete injection mappings configured via @Provides blocks below
    }

    @Provides
    @Singleton
    public ObservableList<LogEntry> provideLogData() {
        return FXCollections.observableArrayList();
    }

    @Provides
    @Singleton
    public MetricRegistry provideMetricRegistry() {
        return new MetricRegistry();
    }

    @Provides
    @Singleton
    public AppConfig provideAppConfig() {
        return new AppConfig();
    }

    @Provides
    @Singleton
    public LogDirectoryWatcher provideWatcher(ObservableList<LogEntry> logData, MetricRegistry metrics) {
        return new LogDirectoryWatcher(logData, metrics);
    }

    @Provides
    @Singleton
    public DockerEngineManager provideDockerEngineManager() {
        // Passing the required container name token string straight to the constructor
        return new DockerEngineManager("timberstrata");
    }

    @Provides
    @Singleton
    public AiAnalysisService provideAiAnalysisService(AppConfig config) {
        // Guice will automatically resolve the AppConfig provider from above
        // and inject it into the service layer here
        return new AiAnalysisService(config);
    }
}