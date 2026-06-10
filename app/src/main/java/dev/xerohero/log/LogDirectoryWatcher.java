package dev.xerohero.log;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.xerohero.MetricRegistry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

/**
 * Background engine that monitors workspace folders for new log lines,
 * automatically parses them, and pushes them safely into the JavaFX UI buffer.
 */
@Singleton
public class LogDirectoryWatcher {

    private final ObservableList<LogEntry> logData;
    private final MetricRegistry metrics;
    private Thread watcherThread;
    private boolean running = false;

    @Inject
    public LogDirectoryWatcher(ObservableList<LogEntry> logData, MetricRegistry metrics) {
        this.logData = logData;
        this.metrics = metrics;
    }

    /**
     * Shuts down any existing file stream and starts tracing the new directory.
     */
    public synchronized void setTargetDirectory(File directory) {
        stopWatching();

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            System.out.println("⚠️ [WATCHER LOG] Invalid log target workspace path provided.");
            return;
        }

        running = true;
        watcherThread = new Thread(() -> watchLoop(directory), "LogTailerEngineThread");
        watcherThread.setDaemon(true);
        watcherThread.start();

        System.out.println("⚡ [WATCHER LOG] Background stream thread active for path: " + directory.getAbsolutePath());
    }

    private void watchLoop(File directory) {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path path = directory.toPath();
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

            // 🏆 SCAN TARGET FOLDER: Load existing files on initial mount
            scanExistingLogFiles(directory);

            while (running) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changedFile = (Path) event.context();
                    File absoluteTarget = new File(directory, changedFile.toString());

                    // 🏆 FIX 1: Allow reading any modified file, bypassing strict .log extension rules
                    if (absoluteTarget.isFile() && !changedFile.toString().startsWith(".")) {
                        System.out.println("📝 [WATCHER LOG] Activity detected on file: " + changedFile);
                        tailFile(absoluteTarget);
                    }
                }
                if (!key.reset()) break;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("ℹ️ [WATCHER LOG] File stream loop closed.");
        }
    }

    private void scanExistingLogFiles(File directory) {
        File[] files = directory.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
        if (files == null) return;

        System.out.println("📂 [WATCHER LOG] Launching workspace directory scan. Found " + files.length + " candidate targets.");

        for (File file : files) {
            System.out.println("📖 [WATCHER LOG] Processing file historical index lines from: " + file.getName());
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parseAndAppendRow(line);
                }
            } catch (IOException e) {
                System.err.println("❌ [WATCHER LOG ERROR] Failed reading file historical index block: " + file.getName());
            }
        }
    }

    private void tailFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseAndAppendRow(line);
            }
        } catch (IOException e) {
            System.err.println("❌ [WATCHER LOG ERROR] Failed tracking stream trail update layout: " + file.getName());
        }
    }

    /**
     * Maps raw text strings into structured LogEntry models and updates the UI buffer.
     */
    private void parseAndAppendRow(String rawLine) {
        if (rawLine == null || rawLine.strip().isEmpty()) return;

        // 🔍 TERMINAL DIAGNOSTIC: Ensures lines are actually breaking past processing blocks
        System.out.println("🚀 [PARSER LOG SUCCESS] Pipeline ingested line -> " + rawLine.trim());

        try {
            // Tokenizer strategy: "TIMESTAMP LEVEL MESSAGE"
            String[] tokens = rawLine.split(" ", 3);
            String timestamp = tokens.length > 0 ? tokens[0] : "UNKNOWN";
            String level = tokens.length > 1 ? tokens[1] : "INFO";
            String message = tokens.length > 2 ? tokens[2] : rawLine;

            LogEntry entry = new LogEntry(timestamp, level, message);

            if (metrics != null) {
                metrics.analyzeAndRegister(entry);
            }

            Platform.runLater(() -> logData.add(entry));

        } catch (Exception e) {
            // Fallback for clean unformatted plain text structures
            LogEntry fallbackEntry = new LogEntry("TRACE", "INFO", rawLine);
            Platform.runLater(() -> logData.add(fallbackEntry));
        }
    }

    public synchronized void stopWatching() {
        running = false;
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
    }
}