# syntax=docker/dockerfile:1

# ---- Build: compile and package with the Maven wrapper, then split the jar into layers ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first: this layer is reused until pom.xml or the wrapper change
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests package \
 && cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- Runtime: JRE only, non-root, one layer per Spring Boot jar layer (dependencies change least) ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system --gid 10001 spring \
 && useradd --system --uid 10001 --gid spring --no-create-home --shell /usr/sbin/nologin spring

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring:spring

# PostgreSQL profile with JSON logs and the actuator on its own port (see application-container.properties).
# Datasource, issuer and JWKS URLs are expected from the environment (see docker-compose.yml).
ENV SPRING_PROFILES_ACTIVE=postgres,container \
    MANAGEMENT_SERVER_PORT=8081

# 8080: API, 8081: actuator (health, Prometheus); publish only 8080 outside the internal network
EXPOSE 8080 8081

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:${MANAGEMENT_SERVER_PORT}/actuator/health/readiness" || exit 1

# Heap sized from the container memory limit; crash (and let the orchestrator restart) instead of limping on OOM.
# Extra flags can be passed with JAVA_TOOL_OPTIONS.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-XX:+UseG1GC", \
            "org.springframework.boot.loader.launch.JarLauncher"]
