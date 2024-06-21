FROM eclipse-temurin:21-jdk-alpine as build
COPY . .
RUN ./gradlew :publisher:bootJar

FROM eclipse-temurin:21-jre-alpine
COPY --from=build publisher/build/libs/publisher-1.0.jar publisher-1.0.jar
CMD ["java", "-jar", "publisher-1.0.jar"]
