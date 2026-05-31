# TimberStrata Engine

A high-performance, containerized data-engineering pipeline built in Java 17 to ingest, monitor, and parse raw unstructured application logs into structured JSON objects in real time. 

The architecture is fully decoupled, utilizing a lightweight multi-stage Docker configuration optimized for cross-platform deployments (including Apple Silicon and native Linux environments).

---

## Architecture Overview

The pipeline establishes a live filesystem bridge between the host machine and an isolated Docker container subsystem. 



1. **Host Storage:** Application components or external streams append raw text strings to a local directory (`/logs`).
2. **Bind Mount Bridge:** A Docker volume map mirrors filesystem updates directly across the OS hypervisor layer into the container runtime.
3. **Hybrid Polling Engine:** A lightweight background thread tracks precise byte-size fingerprints of target logs every 2 seconds to bypass hypervisor notification loss, automatically processing only newly appended line batches.
4. **Regex Extraction:** Raw strings are extracted, tokenized by structural boundaries, and converted into highly maintainable JSON tracking data.

---

## Core Components

* **Multi-Stage Build Engine:** Separates compilation environments (`Maven 3.9.6`) from production-lean execution layers (`Eclipse Temurin 17 JRE on Ubuntu Jammy`) to ensure minimal image footprints and enhanced runtime security.
* **Non-Root Execution:** Implements strict security standards by dropping root privileges inside the container, running the execution daemon under a custom isolation profile (`appuser`).
* **Manifest-Driven Packaging:** Structured via the `maven-jar-plugin` to embed initialization vectors directly inside `META-INF/MANIFEST.MF`, eliminating reliance on heavy framework overhead.

---

## Getting Started

### Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher
* **Build Automation:** Apache Maven 3.x
* **Container Subsystem:** Docker Desktop or Docker Engine

### Installation & Deployment

1. **Clone the repository and enter the application module directory:**
   ```bash
   cd TimberStrata/app

```

2. **Compile and build the isolated multi-stage Docker image:**
```bash
docker build -t timberstrata-engine:v1 .

```


3. **Return to the root project workspace directory:**
```bash
cd ..

```


4. **Launch the processing pipeline container:**
```bash
docker run -d \
  --name timberstrata \
  -e WATCH_DIR=/app/container_logs \
  -v "$(pwd)/logs:/app/container_logs" \
  timberstrata-engine:v1

```



---

## Verification & Testing

To observe the real-time interception of log data, attach a live stream directly to the container's standard output pipeline:

```bash
docker logs -f timberstrata

```

Open a separate terminal window and append a raw sample string into your local tracking log:

```bash
echo "2026-05-31 16:20:00 [ERROR] [IngestionEngine] Live pipeline validation pass" >> logs/test.log

```

The streaming log terminal will immediately print out the processed, structured object:

```text
[Processing New Log File]: test.log
  -> Parsed JSON: {level=ERROR, logger=IngestionEngine, message=Live pipeline validation pass, timestamp=2026-05-31 16:20:00}
Finished parsing batch. Structured logs extracted: 1

```

---

## Troubleshooting & Edge Cases

### Cross-Platform File Event Handling

* **Symptom:** Logs appended on a macOS host do not fire standard Linux kernel level event listeners (`inotify`) inside the container.
* **Resolution:** The engine includes a native polling mechanism that cross-references active file byte metrics every 2 seconds, ensuring robust data pipeline updates across different file systems.

### String-Gluing on Appends

* **Symptom:** Multiple independent logs parsed as a single malformed block.
* **Resolution:** Ensure the upstream generation system uses appropriate newline delimiters (`\n`) for trailing logs to avoid text merging on the boundary tokens.

```
