# --- Build stage ---
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Cache gradle wrapper & deps
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies || true

# Build the application
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# --- Run stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
