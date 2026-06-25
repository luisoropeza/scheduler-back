FROM eclipse-temurin:26-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew :gateway-service:bootJar --no-daemon -q

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/gateway-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
