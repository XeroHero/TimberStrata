package dev.xerohero.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Data Core State Management Context Layer for TimberStrata.
 * Manages tracking counters, custom runtime tokens, and active state flags.
 */
public class DashboardModel {

    private final IntegerProperty errorTelemetryCount = new SimpleIntegerProperty(0);
    private final ConcurrentHashMap<String, AtomicInteger> customMetricTags = new ConcurrentHashMap<>();

    /**
     * 🏆 FIX: Increments global system error counters thread-safely.
     */
    public void incrementErrorTelemetryCount() {
        errorTelemetryCount.set(errorTelemetryCount.get() + 1);
    }

    /**
     * 🏆 FIX: Fetches current error summary metrics.
     */
    public int getErrorTelemetryCount() {
        return errorTelemetryCount.get();
    }

    public IntegerProperty errorTelemetryCountProperty() {
        return errorTelemetryCount;
    }

    /**
     * 🏆 FIX: Registers a clean tracking slot for custom matching metrics (e.g. KAFKA).
     */
    public void registerCustomMetricKey(String token) {
        customMetricTags.putIfAbsent(token.toUpperCase(), new AtomicInteger(0));
    }

    /**
     * Increments tracking data context maps if an explicit matching string token hits the engine pipelines.
     */
    public void incrementCustomTagMetric(String token) {
        AtomicInteger count = customMetricTags.get(token.toUpperCase());
        if (count != null) {
            count.incrementAndGet();
        }
    }
}