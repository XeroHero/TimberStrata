package dev.xerohero;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    // Tracks the last processed size of each file to read only new lines
    private static final Map<Path, Long> fileFingerprints = new HashMap<>();

    public static void main(String[] args) {
        String watchDirStr = System.getenv("WATCH_DIR");
        if (watchDirStr == null) {
            watchDirStr = "./logs";
        }

        Path path = Paths.get(watchDirStr);

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Created watch directory at: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create watch directory: " + e.getMessage());
            return;
        }

        System.out.println("TimberStrata Engine started (Hybrid Polling Mode). Monitoring: " + path.toAbsolutePath());

        // Core execution loop: Poll the folder every 2 seconds
        while (true) {
            try {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.log")) {
                    for (Path entry : stream) {
                        long currentSize = Files.size(entry);
                        long lastKnownSize = fileFingerprints.getOrDefault(entry, 0L);

                        if (currentSize > lastKnownSize) {
                            // File is new or has been appended to!
                            processLogChanges(entry, lastKnownSize);
                            fileFingerprints.put(entry, currentSize);
                        } else if (currentSize < lastKnownSize) {
                            // File was truncated or reset
                            System.out.println("\n[File Reset Detected]: " + entry.getFileName());
                            processLogChanges(entry, 0L);
                            fileFingerprints.put(entry, currentSize);
                        }
                    }
                }

                // Sleep for 2 seconds between filesystem scans
                Thread.sleep(2000);

            } catch (IOException e) {
                System.err.println("Error scanning directory: " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Watcher Engine interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void processLogChanges(Path filePath, long skipBytes) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            // Fast-forward past the data we've already parsed
            if (skipBytes > 0) {
                reader.skip(skipBytes);
            } else {
                System.out.println("\n[Processing New Log File]: " + filePath.getFileName());
            }

            String line;
            int parsedCount = 0;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                Map<String, String> structuredLog = LogParser.parseLine(line);
                if (structuredLog != null) {
                    parsedCount++;
                    System.out.println("  -> Parsed JSON: " + structuredLog);
                } else {
                    System.out.println("  -> [Malformed/Skipped Line]: " + line);
                }
            }
            if (parsedCount > 0) {
                System.out.println("Finished parsing batch. Structured logs extracted: " + parsedCount);
            }
        } catch (IOException e) {
            System.err.println("Error reading changes from " + filePath.getFileName() + ": " + e.getMessage());
        }
    }
}