package dev.xerohero;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class LogGenerator {

    private static final String[] LEVELS = {"INFO", "INFO", "WARN", "INFO", "ERROR", "DEBUG", "INFO"};
    private static final String[] LOGGERS = {
            "dev.xerohero.core.DockerEngineManager",
            "dev.xerohero.core.LogDirectoryWatcher",
            "dev.xerohero.database.ConnectionPool",
            "dev.xerohero.api.GatewayController",
            "dev.xerohero.auth.TokenValidator"
    };

    private static final String[] INFO_MSGS = {
            "Connection handshake verified successfully.",
            "Heartbeat broadcast transmitted to timberstrata container context.",
            "Buffer flush execution pipeline completed.",
            "Resource utilization metrics synchronized.",
            "Inbound telemetry stream socket established smoothly."
    };

    private static final String[] WARN_MSGS = {
            "Disk I/O throughput approaching configured threshold limit.",
            "Connection pooling layer recycling idle connection stale worker tokens.",
            "API Gateway payload latency exceeded baseline variance parameter.",
            "Local container disk queue depth spiking over nominal metrics."
    };

    private static final String[] ERROR_MSGS = {
            "Database execution transaction aborted: deadlock detected.",
            "Failed to pull external image registry headers. Authentication timeout.",
            "OutOfMemoryError tracking: heap usage allocation exceeded available headroom.",
            "Fatal socket break down: connection reset by peer network layout adapter."
    };

    public static void main(String[] args) {
        // Define target directory and file name matching your dashboard watch target
        String targetDirPath = "./test-logs";
        String targetFileName = "app-stream.log";

        File dir = new File(targetDirPath);
        if (!dir.exists() && dir.mkdirs()) {
            System.out.println("📁 Created mock logging directory target: " + dir.getAbsolutePath());
        }

        File logFile = new File(dir, targetFileName);
        System.out.println("🌲 TimberStrata Test Stream Active.");
        System.out.println("✍️ Writing randomized mock logs to: " + logFile.getAbsolutePath());
        System.out.println("⌨️ Press Ctrl+C at any time to terminate the generator stream.");

        Random random = new Random();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        while (true) {
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {

                String timestamp = LocalTime.now().format(timeFormatter);
                String level = LEVELS[random.nextInt(LEVELS.length)];
                String logger = LOGGERS[random.nextInt(LOGGERS.length)];
                String message = "";

                // Select corresponding textual message structure layout
                switch (level) {
                    case "WARN" -> message = WARN_MSGS[random.nextInt(WARN_MSGS.length)];
                    case "ERROR" -> message = ERROR_MSGS[random.nextInt(ERROR_MSGS.length)];
                    case "DEBUG" -> message = "Internal state dump trace check payload configuration bounds verification.";
                    default -> message = INFO_MSGS[random.nextInt(INFO_MSGS.length)];
                }

                // Append structural log representation line item entry
                pw.printf("%s\t%s\t%s\t%s%n", timestamp, level, logger, message);

                // If it's an error, occasionally append a messy Java stack trace block to test AI parsing!
                if ("ERROR".equals(level) && random.nextBoolean()) {
                    pw.println("\tat dev.xerohero.core.EngineExecutor.process(EngineExecutor.java:42)");
                    pw.println("\tat dev.xerohero.database.ConnectionPool.getConnection(ConnectionPool.java:118)");
                    pw.println("\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)");
                    System.out.print("🚨 Appended diagnostic error stack trace block context. ");
                }

                pw.flush();
                System.out.println("⚡ Log line appended: [" + timestamp + "] " + level);

                // Sleep for a randomized interval between 500ms and 2500ms
                Thread.sleep(500 + random.nextInt(2000));

            } catch (IOException e) {
                System.err.println("❌ Critical failure writing log entry context line to file target: " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("\nStopping log generation gracefully.");
                break;
            }
        }
    }
}