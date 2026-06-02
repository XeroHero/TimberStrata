package dev.xerohero;

import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Singleton
public class AppConfig {
    private final Properties properties = new Properties();

    public AppConfig() {
        // Fallback default setup values
        properties.setProperty("docker.container.name", "timberstrata");
        properties.setProperty("docker.heartbeat.seconds", "2");
        properties.setProperty("watcher.poll.ms", "500");
        properties.setProperty("ui.table.max-rows", "2000");

        // Attempt to load from local external disk first, then fallback to internal resources
        try (InputStream is = Files.newInputStream(Paths.get("config.properties"))) {
            properties.load(is);
            System.out.println("✅ Loaded configuration from external config.properties");
        } catch (IOException e) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null) {
                    properties.load(is);
                    System.out.println("ℹ️ Loaded configuration from internal resources");
                } else {
                    System.out.println("⚠️ No config.properties found. Using built-in defaults.");
                }
            } catch (IOException ignored) {}
        }
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = properties.getProperty(key);
        try {
            return val != null ? Integer.parseInt(val.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}