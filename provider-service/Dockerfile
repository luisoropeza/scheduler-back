FROM eclipse-temurin:26-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew :provider-service:bootJar --no-daemon -q

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/provider-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
