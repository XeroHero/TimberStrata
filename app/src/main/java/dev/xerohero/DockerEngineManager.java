package dev.xerohero;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DockerEngineManager {

    private final String containerName;

    public DockerEngineManager(String containerName) {
        this.containerName = containerName;
    }

    public boolean isContainerRunning() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = new ProcessBuilder();

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
            return false;
        }
    }

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
            System.err.println("Failed cross-platform execution target: " + e.getMessage());
        }
    }
}