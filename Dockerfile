FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B


COPY src ./src
RUN mvn clean package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir -p uploads

COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
