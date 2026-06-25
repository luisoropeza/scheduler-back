FROM eclipse-temurin:26-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew :notification-service:bootJar --no-daemon -q

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/notification-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
