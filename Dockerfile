FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY domain/pom.xml domain/pom.xml
COPY application/pom.xml application/pom.xml
COPY infra/pom.xml infra/pom.xml

RUN mvn -q -DskipTests dependency:go-offline

COPY domain/src domain/src
COPY application/src application/src
COPY infra/src infra/src

RUN mvn -q -DskipTests package -pl infra -am

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S budgetmanager && adduser -S budgetmanager -G budgetmanager

COPY --from=build /workspace/infra/target/infra-0.0.1-SNAPSHOT.jar /app/app.jar

USER budgetmanager

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar"]
