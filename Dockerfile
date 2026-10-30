# One Dockerfile for every Spring Boot service: docker build --build-arg MODULE=loan-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY member-service/pom.xml member-service/
COPY catalog-service/pom.xml catalog-service/
COPY loan-service/pom.xml loan-service/
COPY notification-service/pom.xml notification-service/
ARG MODULE
RUN mvn -q -B -pl ${MODULE} -am dependency:go-offline
COPY . .
RUN mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/app.jar app.jar
ENV TZ=Europe/Istanbul
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
