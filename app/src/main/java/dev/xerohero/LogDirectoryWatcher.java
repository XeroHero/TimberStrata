package dev.xerohero;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import java.io.File;
import java.io.RandomAccessFile;
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
                try {
                    // Index initial sizes so we only grab *new* lines
                    fileSizesMap.put(p, Files.size(p));
                } catch (Exception ignored) {}
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

                    if (currentDirectory == null) {
                        Thread.sleep(1000);
                        continue;
                    }

                    // 1. Check OS Event Bus
                    WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                Path fullPath = currentDirectory.resolve((Path) event.context());
                                checkAndParseFile(fullPath);
                            }
                        }
                        key.reset();
                    }

                    // 2. macOS Fallback: Directly scan files in the directory
                    // to bypass native file system event batching delays!
                    try (Stream<Path> stream = Files.list(currentDirectory)) {
                        stream.filter(Files::isRegularFile).forEach(this::checkAndParseFile);
                    }

                } catch (Exception e) {
                    System.err.println("Watch Core issue: " + e.getMessage());
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void checkAndParseFile(Path fullPath) {
        try {
            if (Files.isRegularFile(fullPath)) {
                long currentSize = Files.size(fullPath);
                long knownSize = fileSizesMap.getOrDefault(fullPath, 0L);

                if (currentSize > knownSize) {
                    parseNewLinesWithSeek(fullPath, knownSize);
                    fileSizesMap.put(fullPath, currentSize);
                } else if (currentSize < knownSize) {
                    // File was truncated or cleared down -> reset tracker pointer
                    fileSizesMap.put(fullPath, currentSize);
                }
            }
        } catch (Exception ignored) {}
    }

    private void parseNewLinesWithSeek(Path path, long seekOffset) {
        // Using RandomAccessFile ensures accurate physical byte seeks on disk
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(seekOffset);
            String line;
            while ((line = raf.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Convert ISO-8859-1 string back to crisp UTF-8 for Mac compatibility
                String utf8Line = new String(line.getBytes("ISO-8859-1"), "UTF-8");

                System.out.println("📬 WATCHER READ LINE: " + utf8Line);
                Map<String, String> parsed = LogParser.parseLine(utf8Line);
                if (parsed != null) {
                    LogEntry entry = new LogEntry(
                            parsed.get("timestamp"),
                            parsed.get("level"),
                            parsed.get("logger"),
                            parsed.get("message")
                    );
                    Platform.runLater(() -> {
                        logData.add(entry);
                        metrics.evaluateEntry(entry);
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing lines via seek: " + e.getMessage());
        }
    }
}