# Build stage: compile and package the Spring Boot jar (tests run in CI/locally, not here)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime stage: JRE only, non-root user
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /app/target/rideshare-0.0.1-SNAPSHOT.jar app.jar
USER appuser
# Size the heap from the container limit (Render free tier has 512 MB)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
