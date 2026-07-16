# syntax=docker/dockerfile:1
# ─────────────────────────────────────────────────────────────────────────────
# Multi-stage build for the Spring Boot backend.
#
# WHY multi-stage: the "build" stage carries the full Maven toolchain + JDK to
# compile and package the app. The final "runtime" stage copies ONLY the built
# jar onto a slim JRE — so the shipped image has no Maven, no source, no compiler.
# Smaller image, smaller attack surface, faster pulls.
# ─────────────────────────────────────────────────────────────────────────────

# --- build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy just the POM first and pre-fetch dependencies. This layer is cached and
# only re-runs when pom.xml changes — so ordinary code edits skip the re-download.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the source and package. Tests run in CI, not here, so we skip them to
# keep image builds fast (-DskipTests).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user, never root. Pre-create the reports directory and
# hand it to that user, so the app can write there even with NO volume mounted
# (e.g. a PaaS free tier without an attached disk). With a volume, it's the mount
# point; either way the non-root user owns it.
RUN useradd --system --uid 1001 spring \
 && mkdir -p /data/reports \
 && chown -R spring:spring /data
USER spring

# The single fat jar produced by the build stage (Spring Boot repackage).
COPY --from=build /app/target/radiology-clinic-backend-0.0.1-SNAPSHOT.jar app.jar

# Report PDFs are written here; docker-compose mounts a volume so they persist.
ENV REPORTS_DIR=/data/reports

EXPOSE 8080

# -Duser.timezone=Asia/Kolkata: Windows/Olson "Asia/Calcutta" is rejected by the
# Postgres JDBC session; pin the JVM zone so slot/appointment times stay correct.
# (The spring-boot:run plugin sets this for local dev; `java -jar` needs it here.)
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]
