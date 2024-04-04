FROM eclipse-temurin:21-jdk-alpine as build
COPY . .
RUN ./gradlew :apigateway:bootJar

FROM eclipse-temurin:21-jre-alpine
COPY --from=build apigateway/build/libs/apigateway-1.0.jar apigateway-1.0.jar
CMD ["java", "-jar", "apigateway-1.0.jar"]
