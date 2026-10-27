# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
# Windows checkouts may contain CRLF line endings.
RUN sed -i 's/\r$//' mvnw
RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -ntp dependency:go-offline

COPY config/ config/
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -ntp -DskipTests package \
    && cp target/kairos-api-postgres-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --gid 10001 kairos \
    && useradd --uid 10001 --gid kairos --no-create-home --shell /usr/sbin/nologin kairos

COPY --from=build --chown=kairos:kairos /workspace/app.jar ./app.jar

USER kairos
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
