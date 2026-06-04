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

    // Persistent tracker for current multi-line exception targets
    private LogEntry lastEntry = null;

    public LogDirectoryWatcher(ObservableList<LogEntry> logData, MetricRegistry metrics) {
        this.logData = logData;
        this.metrics = metrics;
    }

    public void changeWatchedDirectory(File directory) {
        activeDirectoryPath = directory.toPath();
        fileSizesMap.clear();
        lastEntry = null;

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
                    if (currentDirectory == null) { Thread.sleep(1000); continue; }

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

                    try (Stream<Path> stream = Files.list(currentDirectory)) {
                        stream.filter(Files::isRegularFile).forEach(this::checkAndParseFile);
                    }
                } catch (Exception e) { System.err.println("Watch Core issue: " + e.getMessage()); }
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
                    fileSizesMap.put(fullPath, currentSize);
                }
            }
        } catch (Exception ignored) {}
    }

    private void parseNewLinesWithSeek(Path path, long seekOffset) {
        System.out.println("🔄 [WATCHER RUN] Triggered for: " + path.getFileName() + " at offset: " + seekOffset);
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(seekOffset);
            String line;

            while ((line = raf.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    System.out.println("ℹ️ [WATCHER] Skipped empty or whitespace line.");
                    continue;
                }

                String utf8Line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                System.out.println("📖 [WATCHER RAW READ]: " + utf8Line);

                Map<String, String> parsed = LogParser.parseLine(utf8Line);

                if (parsed != null) {
                    System.out.println("✅ [WATCHER MATCH] Header detected! Level: " + parsed.get("level"));
                    LogEntry entry = new LogEntry(
                            parsed.get("timestamp"),
                            parsed.get("level"),
                            parsed.get("logger"),
                            parsed.get("message")
                    );
                    lastEntry = entry;

                    Platform.runLater(() -> {
                        logData.add(entry);
                        metrics.evaluateEntry(entry);
                    });
                } else {
                    System.out.println("❓ [WATCHER NO-MATCH] Checking stack trace state...");
                    if (lastEntry != null && (utf8Line.startsWith("\t") || utf8Line.startsWith("    ") || utf8Line.trim().startsWith("at "))) {
                        System.out.println("🔗 [WATCHER AGGREGATE] Appending trace line to: " + lastEntry.timestampProperty().get());
                        LogEntry target = lastEntry;
                        String currentMsg = target.messageProperty().get();
                        String updatedMsg = (currentMsg == null) ? utf8Line : currentMsg + "\n" + utf8Line;

                        Platform.runLater(() -> target.messageProperty().set(updatedMsg));
                    } else {
                        System.out.println("❌ [WATCHER DROP] Dropped line completely. (lastEntry status: " + (lastEntry == null ? "NULL" : "ACTIVE") + ")");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("💥 [WATCHER EXCEPTION] Error in seek parsing loop: " + e.getMessage());
            e.printStackTrace();
        }
    }}