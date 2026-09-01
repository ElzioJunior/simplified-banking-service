FROM maven:3.9.16-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring
USER spring:spring

WORKDIR /application
COPY --from=build --chown=spring:spring /workspace/target/*.jar application.jar

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "application.jar"]
