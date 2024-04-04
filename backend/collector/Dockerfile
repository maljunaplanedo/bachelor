FROM eclipse-temurin:21-jdk-alpine as build
COPY . .
RUN ./gradlew :collector:bootJar

FROM eclipse-temurin:21-jre-alpine
COPY --from=build collector/build/libs/collector-1.0.jar collector-1.0.jar
CMD ["java", "-jar", "collector-1.0.jar"]
