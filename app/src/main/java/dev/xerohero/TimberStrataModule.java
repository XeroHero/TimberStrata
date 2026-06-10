package dev.xerohero;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.xerohero.log.LogDirectoryWatcher;
import dev.xerohero.log.LogEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import dev.xerohero.ai.AiAnalysisService;
import dev.xerohero.ui.DashboardView;
import dev.xerohero.ui.DashboardPresenter;

/**
 * The Dependency Injection Wiring Blueprint for the TimberStrata Engine.
 * Configures scopes, structural singletons, and custom provider pipelines.
 */
public class TimberStrataModule extends AbstractModule {

    @Override
    protected void configure() {
        // Core application singleton infrastructure tracking bindings
        bind(AppConfig.class).in(Singleton.class);
        bind(MetricRegistry.class).in(Singleton.class);
        bind(AiAnalysisService.class).in(Singleton.class);

        // UI Component Frame architecture strategy mapping
        bind(DashboardView.class).in(Singleton.class);
        bind(DashboardPresenter.class).in(Singleton.class);
    }

    /**
     * Explicitly constructs the LogDirectoryWatcher to resolve the Guice missing-constructor error.
     */
    @Provides
    @Singleton
    public LogDirectoryWatcher provideLogDirectoryWatcher(ObservableList<LogEntry> logData, MetricRegistry metrics) {
        return new LogDirectoryWatcher(logData, metrics);
    }

    /**
     * Explicitly constructs the DockerEngineManager, pulling the required AppConfig object out of the container.
     */
    @Provides
    @Singleton
    public DockerEngineManager provideDockerEngineManager(AppConfig config) {
        // 🏆 FIX: Pass the actual AppConfig object instead of a hardcoded String literal
        return new DockerEngineManager(config);
    }

    /**
     * Provides the backing global synchronized buffer array for streaming live log segments.
     * Shared as a thread-safe singleton observable list across the model context layer.
     */
    @Provides
    @Singleton
    public ObservableList<LogEntry> provideRawLogBuffer() {
        return FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    }
}