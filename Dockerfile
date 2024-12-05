FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests --no-transfer-progress

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 