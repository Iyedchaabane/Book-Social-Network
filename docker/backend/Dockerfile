# --- Build Stage ---
FROM maven:3.8.7-openjdk-18 AS build

WORKDIR /app

# Copy only pom.xml to leverage layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -T 1C

# --- Runtime Stage ---
FROM amazoncorretto:17-alpine

# Build arguments
ARG PROFILE=dev
ARG APP_VERSION=1.0.0

# Metadata labels
LABEL org.opencontainers.image.title="Book Network API" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.vendor="BSN"

# Install wget for healthcheck
RUN apk add --no-cache wget

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

WORKDIR /app

# Copy JAR from build stage (FIXED PATH)
COPY --from=build --chown=spring:spring /app/target/book-network-*.jar /app/app.jar
# Expose port
EXPOSE 8088

# Environment variables with sensible defaults
ENV DB_URL=jdbc:postgresql://postgres-sql-bsn:5432/book_social_network \
    ACTIVE_PROFILE=${PROFILE} \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8088/actuator/health || exit 1

# ENTRYPOINT Expert : Utilisation de 'exec' pour que Java soit le PID 1
# Cela résout le warning "JSONArgsRecommended" et gère les signaux d'arrêt correctement.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]