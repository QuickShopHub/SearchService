FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Шаг 2: запуск приложения на JDK slim
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/SearchService-1.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]