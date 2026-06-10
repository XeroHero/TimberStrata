package dev.xerohero;

import com.google.inject.Singleton;
import java.io.*;
import java.util.Properties;

@Singleton
public class AppConfig {

    // 🏆 FIXED: Explicitly anchor the file to user home directory to survive Maven sub-module path shifts
// 🏆 FIXED: Named exactly config.properties, isolated inside user home
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + "config.properties";    private final Properties properties = new Properties();
    private boolean darkMode = false;

    public AppConfig() {
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(CONFIG_FILE);
        System.out.println("🔍 [CONFIG LOG] Absolute configuration path target: " + file.getAbsolutePath());

        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
                if (properties.containsKey("dark-mode")) {
                    darkMode = Boolean.parseBoolean(properties.getProperty("dark-mode"));
                }
                System.out.println("✨ [CONFIG LOG] Successfully loaded dark-mode value from user home: " + darkMode);
            } catch (IOException e) {
                System.err.println("⚠️ [CONFIG] Failed to load tracking properties, using defaults.");
            }
        } else {
            System.out.println("📂 [CONFIG LOG] Config file absent at user home boundary. Initializing factory defaults.");
            saveConfig();
        }
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        saveConfig();
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void saveConfig() {
        properties.setProperty("dark-mode", String.valueOf(darkMode));
        File file = new File(CONFIG_FILE);
        try (OutputStream output = new FileOutputStream(file)) {
            properties.store(output, "TimberStrata User Engine Configurations Profile Framework");
            System.out.println("💾 [CONFIG LOG] Successfully flushed state back to user home configuration target file!");
        } catch (IOException e) {
            System.err.println("❌ [CONFIG] Critical IO lock error saving settings payload.");
        }
    }
}