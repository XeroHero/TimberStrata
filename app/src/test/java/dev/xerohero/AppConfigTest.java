package dev.xerohero;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    @DisplayName("Should cleanly serve fallback system variables if external configuration file is unreadable")
    void testFallbackDefaultCapabilities() {
        AppConfig config = new AppConfig(); // Loads built-in properties mapping map defaults

        String container = config.getString("docker.container.name", "fallback-box");
        int maxRows = config.getInt("ui.table.max-rows", 500);
        int invalidKey = config.getInt("non.existent.key", 999);

        assertEquals("timberstrata", container);
        assertEquals(2000, maxRows);
        assertEquals(999, invalidKey, "Missing keys should gracefully pass down the provided contextual default value.");
    }
}
