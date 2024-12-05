FROM gradle:7.4-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 