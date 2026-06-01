package dev.xerohero;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class LogDirectoryWatcher {
    private final ObservableList<LogEntry> logData;
    private final MetricRegistry metrics;
    private final Map<Path, Long> fileSizesMap = new ConcurrentHashMap<>();
    private volatile Path activeDirectoryPath = null;
    private WatchService watchService;

    public LogDirectoryWatcher(ObservableList<LogEntry> logData, MetricRegistry metrics) {
        this.logData = logData;
        this.metrics = metrics;
    }

    public void changeWatchedDirectory(File directory) {
        activeDirectoryPath = directory.toPath();
        fileSizesMap.clear();

        try (Stream<Path> stream = Files.list(activeDirectoryPath)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                try { fileSizesMap.put(p, Files.size(p)); } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            System.err.println("Indexing error: " + e.getMessage());
        }
    }

    public void startLoop() {
        Thread worker = new Thread(() -> {
            Path currentDirectory = null;
            while (true) {
                try {
                    if (activeDirectoryPath != currentDirectory) {
                        currentDirectory = activeDirectoryPath;
                        if (currentDirectory != null) {
                            if (watchService != null) watchService.close();
                            watchService = FileSystems.getDefault().newWatchService();
                            currentDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                        }
                    }
                    if (currentDirectory == null) { Thread.sleep(1500); continue; }

                    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                Path fullPath = currentDirectory.resolve((Path) event.context());
                                if (Files.isRegularFile(fullPath)) {
                                    long currentSize = Files.size(fullPath);
                                    long knownSize = fileSizesMap.getOrDefault(fullPath, 0L);

                                    if (currentSize > knownSize) {
                                        parseNewLines(fullPath, knownSize);
                                        fileSizesMap.put(fullPath, currentSize);
                                    }
                                }
                            }
                        }
                        key.reset();
                    }
                } catch (Exception e) { System.err.println("Watch Core issue: " + e.getMessage()); }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void parseNewLines(Path path, long skipBytes) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            reader.skip(skipBytes);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Map<String, String> parsed = LogParser.parseLine(line);
                if (parsed != null) {
                    LogEntry entry = new LogEntry(parsed.get("timestamp"), parsed.get("level"), parsed.get("logger"), parsed.get("message"));
                    Platform.runLater(() -> {
                        logData.add(0, entry);
                        metrics.evaluateEntry(entry);
                    });
                }
            }
        } catch (Exception ignored) {}
    }
}