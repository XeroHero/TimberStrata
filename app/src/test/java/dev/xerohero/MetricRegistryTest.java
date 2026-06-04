package dev.xerohero;

import dev.xerohero.core.LogEntry;
import dev.xerohero.core.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetricRegistryTest {

    private MetricRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MetricRegistry();
    }

    @Test
    @DisplayName("Should increment error count accurately when ERROR log level is evaluated")
    void testErrorTracking() {
        LogEntry errorEntry = new LogEntry("10:00:00", "ERROR", "dev.xerohero.Engine", "Database execution failed");
        LogEntry infoEntry = new LogEntry("10:00:01", "INFO", "dev.xerohero.Engine", "Connection alive");

        registry.evaluateEntry(errorEntry);
        registry.evaluateEntry(infoEntry);

        assertEquals(1, registry.errorCountProperty().get(), "Error tracker count should strictly be 1.");
    }

    @Test
    @DisplayName("Should strictly match custom tags without bleed containment errors")
    void testStrictTokenMatching() {
        // Register standard tracking metric tag
        registry.registerTag("DEBUG");

        // Create log entry containing a substring extension token ("DEBUGBLA")
        LogEntry bleedEntry = new LogEntry("10:00:00", "DEBUGBLA", "dev.xerohero.Engine", "System diagnostic run");
        LogEntry exactEntry = new LogEntry("10:00:01", "DEBUG", "dev.xerohero.Engine", "Cache cleared down");

        registry.evaluateEntry(bleedEntry);
        assertEquals(0, registry.getCustomCounters().get("DEBUG").get(),
                "Substring extensions (DEBUGBLA) must not increment baseline metrics (DEBUG).");

        registry.evaluateEntry(exactEntry);
        assertEquals(1, registry.getCustomCounters().get("DEBUG").get(),
                "Exact structural severity matches must increment metrics targets.");
    }

    @Test
    @DisplayName("Should reset all properties back to zero cleanly")
    void testResetCapabilities() {
        registry.registerTag("WARN");
        LogEntry errorEntry = new LogEntry("10:00:00", "ERROR", "Engine", "Crash");
        LogEntry warnEntry = new LogEntry("10:00:01", "WARN", "Engine", "Slow disk");

        registry.evaluateEntry(errorEntry);
        registry.evaluateEntry(warnEntry);

        registry.resetAll();

        assertEquals(0, registry.errorCountProperty().get());
        assertEquals(0, registry.getCustomCounters().get("WARN").get());
    }
}
