# ==========================================
# Stage 1: Build the Native Image
# ==========================================
FROM ghcr.io/graalvm/graalvm-community:25 AS builder

WORKDIR /build

# Copy gradle wrapper
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# Copy gradle configuration
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

ENV SPRING_PROFILES_ACTIVE=local
# Build native image
RUN ./gradlew nativeCompile --no-daemon

# ==========================================
# Stage 2: Create Minimal Runtime Image
# ==========================================
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    libc6 libstdc++6 zlib1g \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /application

COPY --from=builder --chown=spring:spring /build/build/native/nativeCompile/bazar-api-gateway ./bazar-api-gateway

RUN chmod +x ./bazar-api-gateway

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["./bazar-api-gateway"]