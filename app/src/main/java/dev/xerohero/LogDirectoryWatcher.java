package dev.xerohero;

import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class LogDirectoryWatcher {
    private final ObservableList<LogEntry> logData;
    private final MetricRegistry metrics;
    private final Map<Path, Long> fileSizesMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private volatile Path activeDirectoryPath = null;
    private WatchService watchService;
    private Thread workerThread;

    @Inject
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
                    fileSizesMap.put(p, Files.size(p));
                } catch (IOException e) {
                    System.err.println("⚠️ Could not read initial size for: " + p.getFileName());
                }
            });
        } catch (IOException e) {
            System.err.println("❌ Critical indexing error on directory swap: " + e.getMessage());
        }
    }

    public synchronized void startLoop() {
        if (isRunning.getAndSet(true)) {
            return;
        }

        workerThread = new Thread(() -> {
            Path currentDirectory = null;

            while (isRunning.get()) {
                try {
                    if (activeDirectoryPath != currentDirectory) {
                        currentDirectory = activeDirectoryPath;
                        if (currentDirectory != null) {
                            safelyCloseWatchService();
                            watchService = FileSystems.getDefault().newWatchService();
                            currentDirectory.register(watchService,
                                    StandardWatchEventKinds.ENTRY_MODIFY,
                                    StandardWatchEventKinds.ENTRY_CREATE
                            );
                        }
                    }

                    if (currentDirectory == null) {
                        TimeUnit.MILLISECONDS.sleep(500);
                        continue;
                    }

                    WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

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

                    boolean valid = key.reset();
                    if (!valid) {
                        System.err.println("⚠️ Watch key invalid. Resetting target.");
                        activeDirectoryPath = null;
                    }

                } catch (ClosedWatchServiceException e) {
                    System.out.println("ℹ️ WatchService closed cleanly during reconfiguration.");
                } catch (InterruptedException e) {
                    System.err.println("⚠️ Background worker loop interrupted. Stopping.");
                    isRunning.set(false);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("💥 Unexpected watch core failure: " + e.getMessage());
                }
            }
        }, "TimberStrata-Watcher-Daemon");

        workerThread.setDaemon(true);
        workerThread.start();
    }

    public synchronized void stopLoop() {
        isRunning.set(false);
        safelyCloseWatchService();
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private void safelyCloseWatchService() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void parseNewLines(Path path, long skipBytes) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            long skipped = 0;
            while (skipped < skipBytes) {
                long currentSkip = reader.skip(skipBytes - skipped);
                if (currentSkip <= 0) break;
                skipped += currentSkip;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                Map<String, String> parsed = LogParser.parseLine(line);
                if (parsed != null) {
                    LogEntry entry = new LogEntry(parsed.get("timestamp"), parsed.get("level"), parsed.get("logger"), parsed.get("message"));

                    Platform.runLater(() -> {
                        logData.add(0, entry);
                        metrics.evaluateEntry(entry);

                        // Memory Ceiling Guard
                        if (logData.size() > 2000) {
                            logData.remove(logData.size() - 1);
                        }
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Failed reading log data delta: " + e.getMessage());
        }
    }
}