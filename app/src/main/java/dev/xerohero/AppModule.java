package dev.xerohero;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import dev.xerohero.log.LogEntry;
import dev.xerohero.ai.AiAnalysisService;

/**
 * Core Dependency Injection Module setting up system mappings across the application.
 */
public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        // 🏆 FORCE SINGLETON LIST PIPELINE BINDING:
        // This explicitly forces Guice to distribute the exact same list instance
        // to DashboardPresenter and LogDirectoryWatcher, bridging the data pipeline gap.
        bind(new com.google.inject.TypeLiteral<ObservableList<LogEntry>>() {})
                .toInstance(FXCollections.observableArrayList());
    }

    @Provides
    @Singleton
    public DockerEngineManager provideDockerEngineManager(AppConfig config) {
        return new DockerEngineManager(config);
    }

    @Provides
    @Singleton
    public AiAnalysisService provideAiAnalysisService(AppConfig config) {
        return new AiAnalysisService(config);
    }
}