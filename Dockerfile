# ==========================================
# STAGE 1: Build the Application
# ==========================================
FROM gradle:9.2-jdk21-ubi AS builder

WORKDIR /app

# 1. Copy dependency definitions first (to cache dependencies)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# 2. Download dependencies.
# This layer is cached unless you change your build.gradle.kts
RUN gradle dependencies --no-daemon

# 3. Copy the actual source code
COPY src ./src

# 4. Build the JAR (Skip tests to save time, assuming tests ran in CI)
RUN gradle bootJar --no-daemon -x test

# 5. Extract JAR layers (Spring Boot optimization)
# This splits the JAR into dependencies vs your code
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# ==========================================
# STAGE 2: Run the Application (The Tiny Image)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 1. Create a non-root user for security (Alpine specific)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 2. Copy the extracted layers from the builder stage
# This is more efficient than copying one fat JAR
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# 3. Expose port (Documentation only)
EXPOSE 8080

# 4. Use Spring Boot's JarLauncher
# Note: For Spring Boot 3.2+, the path is org.springframework.boot.loader.launch.JarLauncher
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]