package dev.xerohero;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import dev.xerohero.log.LogEntry;
import dev.xerohero.ai.AiAnalysisService;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        // 🏆 FORCE GUICE TO SHARE A SINGLE CONFIGPOOL EXTENSION OBJECT
        bind(AppConfig.class).asEagerSingleton();

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