# --- Stage 1: Build the application ---
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application (skip tests for rapid local iteration)
RUN mvn clean package -DskipTests

# --- Stage 2: Create the lean runtime image ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user for security best practices (syntax adjusted for Ubuntu/Debian)
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

# Copy the compiled jar from the builder stage
COPY --from=builder /app/target/TimberStrata-1.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]