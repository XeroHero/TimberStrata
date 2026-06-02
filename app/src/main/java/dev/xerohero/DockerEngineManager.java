package dev.xerohero;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DockerEngineManager {

    private final String containerName;

    /**
     * Initializes the Docker manager for a specific target container.
     * * @param containerName The name of the Docker container to manage (e.g., "timberstrata").
     */
    public DockerEngineManager(String containerName) {
        this.containerName = containerName;
    }

    /**
     * Inspects the local Docker daemon to see if the container is currently running.
     * This method runs independently of any UI framework or threading model.
     *
     * @return true if the container state evaluates explicitly to running, false otherwise.
     */
    public boolean isContainerRunning() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = new ProcessBuilder();

            // Route execution paths based on host environment shell architecture
            if (os.contains("win")) {
                pb.command("cmd.exe", "/c", "docker inspect -f {{.State.Running}} " + containerName);
            } else {
                pb.command("sh", "-c", "docker inspect -f {{.State.Running}} " + containerName);
            }

            Process process = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String out = r.readLine();
                return "true".equals(out != null ? out.trim() : "");
            }
        } catch (Exception ignored) {
            // Fails safe to offline if Docker Desktop is stopped or CLI tools aren't initialized
            return false;
        }
    }

    /**
     * Executes an arbitrary engine control instruction (start, stop, restart)
     * safely inside the native host terminal interpreter.
     *
     * @param cmd The raw CLI instruction string to execute.
     */
    public void executeCommand(String cmd) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder = new ProcessBuilder();

            if (os.contains("win")) {
                processBuilder.command("cmd.exe", "/c", cmd);
            } else {
                processBuilder.command("sh", "-c", cmd);
            }

            processBuilder.start();
        } catch (Exception e) {
            System.err.println("Failed to execute cross-platform action: " + e.getMessage());
        }
    }
}